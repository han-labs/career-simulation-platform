-- P5/P6 additive only. No adoption/backfill of legacy chats, consents, reports or plans.
ALTER TABLE public.agent_session ADD COLUMN creation_request_id uuid;
CREATE UNIQUE INDEX uq_agent_session_creation_request ON public.agent_session(owner_user_id,creation_request_id) WHERE creation_request_id IS NOT NULL;
CREATE TABLE public.agent_execution_context_source (
 execution_id uuid NOT NULL, source_interaction_id uuid NOT NULL, owner_user_id bigint NOT NULL,
 FOREIGN KEY(execution_id,owner_user_id) REFERENCES public.agent_execution(id,initiated_by_user_id) ON DELETE RESTRICT,
 FOREIGN KEY(source_interaction_id,owner_user_id) REFERENCES public.interaction_record(id,owner_user_id) ON DELETE RESTRICT,
 PRIMARY KEY(execution_id,source_interaction_id)
);
CREATE INDEX ix_agent_execution_context_source ON public.agent_execution_context_source(source_interaction_id);
CREATE TABLE public.agent_execution_event (
 execution_id uuid NOT NULL REFERENCES public.agent_execution(id) ON DELETE RESTRICT,
 sequence_number bigint NOT NULL CHECK(sequence_number>0),
 event_type varchar(40) NOT NULL CHECK(event_type IN ('execution.started','tool.status','answer.segment','execution.completed','execution.failed','execution.refused','execution.cancelled')),
 payload jsonb NOT NULL CHECK(jsonb_typeof(payload)='object' AND octet_length(payload::text)<=24000),
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(execution_id,sequence_number)
);
CREATE TABLE public.insight_report (
 id uuid PRIMARY KEY, owner_user_id bigint NOT NULL REFERENCES public.student_profiles(user_id) ON DELETE RESTRICT,
 requested_interaction_id uuid NOT NULL REFERENCES public.interaction_record(id) ON DELETE RESTRICT,
 source_execution_id uuid NOT NULL UNIQUE,
 FOREIGN KEY(source_execution_id,owner_user_id) REFERENCES public.agent_execution(id,initiated_by_user_id) ON DELETE RESTRICT,
 schema_version varchar(40) NOT NULL CHECK(schema_version='careersim-insight-v1'),
 structured_insights jsonb NOT NULL CHECK(jsonb_typeof(structured_insights)='object'),
 evidence_refs jsonb NOT NULL CHECK(jsonb_typeof(evidence_refs)='array'),
 status varchar(20) NOT NULL DEFAULT 'AVAILABLE' CHECK(status IN ('AVAILABLE','DELETED')),
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted_at timestamptz,
 CHECK((status='AVAILABLE' AND deleted_at IS NULL) OR (status='DELETED' AND deleted_at IS NOT NULL))
);
CREATE INDEX ix_insight_report_owner ON public.insight_report(owner_user_id,created_at,id);
ALTER TABLE public.exploration_plan ADD COLUMN source_execution_id uuid,
 ADD COLUMN confirmation_request_id uuid, ADD COLUMN confirmation_hash char(64),
 ADD CONSTRAINT fk_confirmed_plan_execution_owner FOREIGN KEY(source_execution_id,student_id) REFERENCES public.agent_execution(id,initiated_by_user_id) ON DELETE RESTRICT,
 ADD CONSTRAINT ck_confirmed_plan_execution_confirmation CHECK((source_execution_id IS NULL AND confirmation_request_id IS NULL AND confirmation_hash IS NULL) OR (source_execution_id IS NOT NULL AND confirmation_request_id IS NOT NULL AND confirmation_hash IS NOT NULL));
CREATE UNIQUE INDEX uq_confirmed_plan_execution ON public.exploration_plan(source_execution_id) WHERE source_execution_id IS NOT NULL;
CREATE UNIQUE INDEX uq_confirmed_plan_request ON public.exploration_plan(student_id,confirmation_request_id) WHERE confirmation_request_id IS NOT NULL;

CREATE OR REPLACE FUNCTION public.guard_agent_session() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM public.agent a JOIN public.agent_configuration c ON c.id=NEW.configuration_id AND c.agent_id=a.id JOIN public.agent_assignment x ON x.agent_id=a.id AND x.student_user_id=NEW.owner_user_id AND x.status='ACTIVE' JOIN public.app_users u ON u.id=x.student_user_id AND u.status='ACTIVE' AND u.role='STUDENT' WHERE a.id=NEW.agent_id AND a.status='ACTIVE' AND a.published_configuration_id=c.id AND c.status='PUBLISHED' AND c.sealed) THEN RAISE EXCEPTION 'ASSIGNED_CURRENT_PUBLISHED_AGENT_REQUIRED'; END IF;
 ELSE
  IF TG_OP='DELETE' THEN RAISE EXCEPTION 'SESSION_HISTORY_SOFT_DELETE_REQUIRED'; END IF;
  IF NEW.id<>OLD.id OR NEW.owner_user_id<>OLD.owner_user_id OR NEW.agent_id<>OLD.agent_id OR NEW.configuration_id<>OLD.configuration_id OR NEW.started_at<>OLD.started_at OR NEW.creation_request_id IS DISTINCT FROM OLD.creation_request_id THEN RAISE EXCEPTION 'SESSION_OWNER_CONFIG_IMMUTABLE'; END IF;
  IF (OLD.status='DELETED' AND NEW.status<>'DELETED') OR NEW.context_epoch<OLD.context_epoch OR NEW.next_sequence<OLD.next_sequence THEN RAISE EXCEPTION 'SESSION_CANNOT_RESURRECT_OR_REWIND'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE FUNCTION public.guard_agent_interaction() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'INTERACTION_SOFT_DELETE_REQUIRED'; END IF;
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM public.agent_session WHERE id=NEW.session_id AND owner_user_id=NEW.owner_user_id AND status='ACTIVE') THEN RAISE EXCEPTION 'LIVE_OWNED_SESSION_REQUIRED'; END IF;
 ELSE
  IF (to_jsonb(NEW)-'deleted_at'-'execution_id') IS DISTINCT FROM (to_jsonb(OLD)-'deleted_at'-'execution_id') OR (OLD.execution_id IS NOT NULL AND NEW.execution_id IS DISTINCT FROM OLD.execution_id) OR (OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS DISTINCT FROM OLD.deleted_at) THEN RAISE EXCEPTION 'INTERACTION_CONTENT_IMMUTABLE'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER guard_agent_interaction BEFORE INSERT OR UPDATE OR DELETE ON public.interaction_record FOR EACH ROW EXECUTE FUNCTION public.guard_agent_interaction();
CREATE FUNCTION public.guard_agent_source_lineage() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE uid bigint; iid uuid;
BEGIN
 uid:=NEW.owner_user_id;iid:=NEW.source_interaction_id;
 IF NOT EXISTS(SELECT 1 FROM public.interaction_record i JOIN public.agent_session s ON s.id=i.session_id WHERE i.id=iid AND i.owner_user_id=uid AND i.deleted_at IS NULL AND s.status='ACTIVE') THEN RAISE EXCEPTION 'LIVE_OWNED_CONTEXT_SOURCE_REQUIRED'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_agent_summary_source BEFORE INSERT OR UPDATE ON public.agent_summary_source FOR EACH ROW EXECUTE FUNCTION public.guard_agent_source_lineage();
CREATE TRIGGER guard_agent_execution_context_source BEFORE INSERT OR UPDATE ON public.agent_execution_context_source FOR EACH ROW EXECUTE FUNCTION public.guard_agent_source_lineage();
CREATE FUNCTION public.invalidate_deleted_agent_context() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE source_session uuid;
BEGIN
 IF TG_TABLE_NAME='agent_session' THEN source_session:=NEW.id; ELSE source_session:=NEW.session_id; END IF;
 WITH RECURSIVE affected(id) AS (
  SELECT source_session UNION
  SELECT x.target_session_id FROM public.agent_summary_source x JOIN public.interaction_record i ON i.id=x.source_interaction_id JOIN affected a ON a.id=i.session_id
 ) UPDATE public.agent_session s SET context_summary=NULL,summary_status='INVALIDATED',summary_source_refs='[]',summary_updated_at=CURRENT_TIMESTAMP,context_epoch=context_epoch+1,row_version=row_version+1 WHERE s.id IN (SELECT id FROM affected);
 UPDATE public.agent_execution e SET cancellation_requested_at=COALESCE(e.cancellation_requested_at,CURRENT_TIMESTAMP)
 WHERE e.execution_mode='STUDENT_CHAT' AND e.status IN ('ACCEPTED','RUNNING') AND
 (e.session_id=source_session OR EXISTS(SELECT 1 FROM public.agent_execution_context_source x JOIN public.interaction_record i ON i.id=x.source_interaction_id WHERE x.execution_id=e.id AND i.session_id=source_session));
 RETURN NEW;
END $$;
CREATE TRIGGER invalidate_agent_session_context AFTER UPDATE OF deleted_at ON public.agent_session FOR EACH ROW WHEN(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL) EXECUTE FUNCTION public.invalidate_deleted_agent_context();
CREATE TRIGGER invalidate_agent_interaction_context AFTER UPDATE OF deleted_at ON public.interaction_record FOR EACH ROW WHEN(OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL) EXECUTE FUNCTION public.invalidate_deleted_agent_context();
CREATE FUNCTION public.guard_student_execution_epoch() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.execution_mode<>'STUDENT_CHAT' OR NEW.status IN ('CANCELLED','FAILED') OR (NEW.status IN ('ACCEPTED','RUNNING') AND NEW.cancellation_requested_at IS NOT NULL) THEN RETURN NEW; END IF;
 IF NOT EXISTS(SELECT 1 FROM public.agent_session s JOIN public.app_users u ON u.id=s.owner_user_id WHERE s.id=NEW.session_id AND s.status='ACTIVE' AND s.context_epoch=NEW.context_epoch AND u.privacy_version=NEW.privacy_version) THEN RAISE EXCEPTION 'STUDENT_CONTEXT_EPOCH_CHANGED'; END IF;
 IF EXISTS(SELECT 1 FROM public.agent_execution_context_source x JOIN public.interaction_record i ON i.id=x.source_interaction_id JOIN public.agent_session s ON s.id=i.session_id WHERE x.execution_id=NEW.id AND (i.deleted_at IS NOT NULL OR s.status<>'ACTIVE')) THEN RAISE EXCEPTION 'STUDENT_CONTEXT_SOURCE_UNAVAILABLE'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_student_execution_epoch BEFORE INSERT OR UPDATE ON public.agent_execution FOR EACH ROW EXECUTE FUNCTION public.guard_student_execution_epoch();
CREATE FUNCTION public.guard_insight_report() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP<>'INSERT' THEN
  IF TG_OP='DELETE' OR (to_jsonb(NEW)-'status'-'deleted_at') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'deleted_at') OR OLD.status='DELETED' THEN RAISE EXCEPTION 'INSIGHT_REPORT_IMMUTABLE'; END IF;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM public.agent_execution e JOIN public.agent_session s ON s.id=e.session_id WHERE e.id=NEW.source_execution_id AND e.initiated_by_user_id=NEW.owner_user_id AND e.input_interaction_id=NEW.requested_interaction_id AND e.execution_mode='STUDENT_CHAT' AND e.task_type='INSIGHT_REPORT' AND e.status='SUCCEEDED' AND s.status='ACTIVE' AND e.input_snapshot->>'explicitRequest'='true' AND e.output_snapshot->'report'=NEW.structured_insights AND NEW.structured_insights->>'schemaVersion'=NEW.schema_version AND NEW.structured_insights->'evidenceRefs'=NEW.evidence_refs) THEN RAISE EXCEPTION 'EXPLICIT_VALID_OWNED_REPORT_EXECUTION_REQUIRED'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER guard_insight_report BEFORE INSERT OR UPDATE OR DELETE ON public.insight_report FOR EACH ROW EXECUTE FUNCTION public.guard_insight_report();
CREATE FUNCTION public.guard_confirmed_plan_source() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='UPDATE' AND OLD.source_execution_id IS NOT NULL THEN
  IF NEW.source_execution_id IS DISTINCT FROM OLD.source_execution_id OR NEW.confirmation_request_id IS DISTINCT FROM OLD.confirmation_request_id OR NEW.confirmation_hash IS DISTINCT FROM OLD.confirmation_hash OR NEW.title<>OLD.title OR NEW.steps<>OLD.steps OR NEW.student_id<>OLD.student_id OR NEW.confirmed_at<>OLD.confirmed_at THEN RAISE EXCEPTION 'CONFIRMED_PLAN_SOURCE_IMMUTABLE'; END IF;
  RETURN NEW;
 END IF;
 IF NEW.source_execution_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.agent_execution e JOIN public.agent_session s ON s.id=e.session_id WHERE e.id=NEW.source_execution_id AND e.initiated_by_user_id=NEW.student_id AND e.execution_mode='STUDENT_CHAT' AND e.task_type='PLAN_SUGGESTION' AND e.status='SUCCEEDED' AND s.status='ACTIVE' AND e.output_snapshot->>'planContentHash'=NEW.confirmation_hash AND e.output_snapshot->'planSuggestion'->>'title'=NEW.title AND e.output_snapshot->'planSuggestion'->'steps'=NEW.steps) THEN RAISE EXCEPTION 'EXPLICIT_VALID_OWNED_PLAN_EXECUTION_REQUIRED'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_confirmed_plan_source BEFORE INSERT OR UPDATE ON public.exploration_plan FOR EACH ROW EXECUTE FUNCTION public.guard_confirmed_plan_source();
