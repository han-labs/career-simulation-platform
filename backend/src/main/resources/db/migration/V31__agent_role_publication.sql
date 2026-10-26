-- Role publication replaces per-Student assignment discovery; historical rows remain intact.
CREATE TABLE public.agent_role_binding (
 role varchar(24) PRIMARY KEY CHECK(role IN ('STUDENT','ENTERPRISE')),
 agent_id uuid REFERENCES public.agent(id) ON DELETE RESTRICT,
 row_version bigint NOT NULL DEFAULT 0 CHECK(row_version>=0),
 updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO public.agent_role_binding(role) VALUES ('STUDENT'),('ENTERPRISE');
CREATE FUNCTION public.guard_agent_role_binding() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ROLE_SLOT_CANNOT_BE_DELETED'; END IF;
 IF TG_OP='UPDATE' AND (NEW.role<>OLD.role OR NEW.row_version<>OLD.row_version+1) THEN RAISE EXCEPTION 'ROLE_REVISION_REQUIRED'; END IF;
 IF NEW.agent_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.agent a JOIN public.agent_configuration c ON c.id=a.published_configuration_id AND c.agent_id=a.id AND c.status='PUBLISHED' AND c.sealed WHERE NOT EXISTS(SELECT 1 FROM public.agent_config_kb_scope g JOIN public.knowledge_base kb ON kb.id=g.knowledge_base_id WHERE g.configuration_id=c.id AND kb.status<>'ACTIVE') AND a.id=NEW.agent_id AND a.status='ACTIVE' AND EXISTS(SELECT 1 FROM public.agent_execution e WHERE e.configuration_id=c.id AND e.configuration_hash=c.content_hash AND e.execution_mode='ADMIN_PREVIEW' AND e.status='SUCCEEDED')) THEN RAISE EXCEPTION 'ROLE_REQUIRES_READY_PUBLISHED_AGENT'; END IF;
 RETURN NEW; END $$;
CREATE TRIGGER guard_agent_role_binding BEFORE INSERT OR UPDATE OR DELETE ON public.agent_role_binding FOR EACH ROW EXECUTE FUNCTION public.guard_agent_role_binding();
CREATE FUNCTION public.clear_unavailable_agent_roles() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 UPDATE public.agent_role_binding b SET agent_id=NULL,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP
 WHERE b.agent_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.agent a JOIN public.agent_configuration c ON c.id=a.published_configuration_id AND c.agent_id=a.id AND c.status='PUBLISHED' AND c.sealed WHERE a.id=b.agent_id AND a.status='ACTIVE' AND NOT EXISTS(SELECT 1 FROM public.agent_config_kb_scope g JOIN public.knowledge_base kb ON kb.id=g.knowledge_base_id WHERE g.configuration_id=c.id AND kb.status<>'ACTIVE'));
 RETURN NEW;END $$;
CREATE TRIGGER clear_agent_roles AFTER UPDATE OF status,published_configuration_id ON public.agent FOR EACH ROW EXECUTE FUNCTION public.clear_unavailable_agent_roles();
CREATE TRIGGER clear_config_roles AFTER UPDATE OF status ON public.agent_configuration FOR EACH ROW EXECUTE FUNCTION public.clear_unavailable_agent_roles();
CREATE TRIGGER clear_kb_roles AFTER UPDATE OF status ON public.knowledge_base FOR EACH ROW EXECUTE FUNCTION public.clear_unavailable_agent_roles();
CREATE OR REPLACE FUNCTION public.guard_agent_session() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM public.agent a JOIN public.agent_configuration c ON c.id=NEW.configuration_id AND c.agent_id=a.id JOIN public.agent_role_binding x ON x.agent_id=a.id AND x.role='STUDENT' JOIN public.app_users u ON u.id=NEW.owner_user_id AND u.status='ACTIVE' AND u.role='STUDENT' WHERE a.id=NEW.agent_id AND a.status='ACTIVE' AND a.published_configuration_id=c.id AND c.status='PUBLISHED' AND c.sealed) THEN RAISE EXCEPTION 'ROLE_CURRENT_PUBLISHED_AGENT_REQUIRED'; END IF;
 ELSE
  IF TG_OP='DELETE' THEN RAISE EXCEPTION 'SESSION_HISTORY_SOFT_DELETE_REQUIRED'; END IF;
  IF NEW.id<>OLD.id OR NEW.owner_user_id<>OLD.owner_user_id OR NEW.agent_id<>OLD.agent_id OR NEW.configuration_id<>OLD.configuration_id OR NEW.started_at<>OLD.started_at OR NEW.creation_request_id IS DISTINCT FROM OLD.creation_request_id THEN RAISE EXCEPTION 'SESSION_OWNER_CONFIG_IMMUTABLE'; END IF;
  IF (OLD.status='DELETED' AND NEW.status<>'DELETED') OR NEW.context_epoch<OLD.context_epoch OR NEW.next_sequence<OLD.next_sequence THEN RAISE EXCEPTION 'SESSION_CANNOT_RESURRECT_OR_REWIND'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION public.guard_agent_execution() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE owner_id bigint; pinned uuid; session_status text; actor_role text; actor_status text;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'EXECUTION_HISTORY_IMMUTABLE'; END IF;
 IF TG_OP='UPDATE' THEN
  IF OLD.status IN ('SUCCEEDED','REFUSED','FAILED','CANCELLED') THEN RAISE EXCEPTION 'TERMINAL_EXECUTION_IMMUTABLE'; END IF;
  IF (to_jsonb(NEW)-'status'-'refusal_or_error_code'-'tool_steps_and_sources'-'output_snapshot'-'pricing_snapshot'-'input_tokens'-'output_tokens'-'estimated_cost'-'cost_status'-'latency_ms'-'finished_at'-'cancellation_requested_at') IS DISTINCT FROM
     (to_jsonb(OLD)-'status'-'refusal_or_error_code'-'tool_steps_and_sources'-'output_snapshot'-'pricing_snapshot'-'input_tokens'-'output_tokens'-'estimated_cost'-'cost_status'-'latency_ms'-'finished_at'-'cancellation_requested_at') THEN RAISE EXCEPTION 'EXECUTION_INPUT_IMMUTABLE'; END IF;
  IF OLD.cancellation_requested_at IS NOT NULL AND NEW.cancellation_requested_at IS DISTINCT FROM OLD.cancellation_requested_at THEN RAISE EXCEPTION 'CANCELLATION_INTENT_IMMUTABLE'; END IF;
  IF NEW.cancellation_requested_at IS NOT NULL AND NEW.status='SUCCEEDED' THEN RAISE EXCEPTION 'CANCELLED_EXECUTION_CANNOT_SUCCEED'; END IF;
  IF OLD.status='RUNNING' AND NEW.status='ACCEPTED' THEN RAISE EXCEPTION 'EXECUTION_STATUS_INVALID'; END IF;
  -- Cleanup must remain possible after revocation/lock; it cannot publish an answer.
  IF NEW.status IN ('FAILED','CANCELLED') THEN RETURN NEW; END IF;
 END IF;
 SELECT role,status INTO actor_role,actor_status FROM public.app_users WHERE id=NEW.initiated_by_user_id;
 IF actor_status<>'ACTIVE' THEN RAISE EXCEPTION 'ACTIVE_EXECUTION_ACTOR_REQUIRED'; END IF;
 IF NEW.configuration_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.agent_configuration WHERE id=NEW.configuration_id AND sealed AND content_hash=NEW.configuration_hash) THEN RAISE EXCEPTION 'SEALED_CONFIGURATION_REQUIRED'; END IF;
 IF NEW.execution_mode IN ('ADMIN_PREVIEW','KB_TEST') THEN
  IF actor_role<>'ADMINISTRATOR' THEN RAISE EXCEPTION 'ACTIVE_ADMIN_REQUIRED'; END IF;
 ELSE
  SELECT owner_user_id,configuration_id,status INTO owner_id,pinned,session_status FROM public.agent_session WHERE id=NEW.session_id FOR UPDATE;
  IF actor_role<>'STUDENT' OR owner_id IS DISTINCT FROM NEW.initiated_by_user_id OR pinned IS DISTINCT FROM NEW.configuration_id OR session_status<>'ACTIVE' OR NOT EXISTS(SELECT 1 FROM public.interaction_record WHERE id=NEW.input_interaction_id AND session_id=NEW.session_id AND owner_user_id=owner_id AND actor_type='USER' AND deleted_at IS NULL) THEN RAISE EXCEPTION 'EXECUTION_SESSION_OWNER_INPUT_REQUIRED'; END IF;
  PERFORM 1 FROM public.agent_session s JOIN public.agent a ON a.id=s.agent_id AND a.status='ACTIVE' JOIN public.agent_role_binding rb ON rb.agent_id=a.id AND rb.role='STUDENT' JOIN public.agent_configuration current_config ON current_config.id=a.published_configuration_id AND current_config.status='PUBLISHED' AND current_config.sealed WHERE s.id=NEW.session_id FOR SHARE OF rb; IF NOT FOUND THEN RAISE EXCEPTION 'ACTIVE_ROLE_AGENT_REQUIRED'; END IF;
 END IF;
 IF NEW.execution_mode='KB_TEST' AND NEW.configuration_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.agent_config_kb_scope WHERE configuration_id=NEW.configuration_id AND knowledge_base_id=NEW.knowledge_base_id) THEN RAISE EXCEPTION 'CONFIGURATION_KB_GRANT_REQUIRED'; END IF;
 RETURN NEW;
END $$;
