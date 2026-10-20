-- Additive P3/P4. Existing Core identifiers, knowledge vectors and histories stay intact.
CREATE TABLE public.knowledge_base (
 id uuid PRIMARY KEY, name varchar(180) NOT NULL CHECK(length(btrim(name))>0),
 scope_type varchar(20) NOT NULL CHECK(scope_type IN ('PLATFORM','ENTERPRISE')),
 enterprise_id bigint REFERENCES public.enterprise(id) ON DELETE RESTRICT,
 visibility varchar(20) NOT NULL DEFAULT 'INTERNAL' CHECK(visibility='INTERNAL'),
 status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','HIDDEN','ARCHIVED')),
 publication_revision bigint NOT NULL DEFAULT 0 CHECK(publication_revision>=0),
 created_by_admin_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 row_version bigint NOT NULL DEFAULT 0 CHECK(row_version>=0),
 CHECK((scope_type='PLATFORM' AND enterprise_id IS NULL) OR (scope_type='ENTERPRISE' AND enterprise_id IS NOT NULL))
);
CREATE INDEX ix_knowledge_base_enterprise ON public.knowledge_base(enterprise_id);
ALTER TABLE public.knowledge_versions ADD CONSTRAINT uq_knowledge_version_parent UNIQUE(document_id,id);
ALTER TABLE public.knowledge_documents ADD CONSTRAINT fk_active_knowledge_same_document
 FOREIGN KEY(id,active_version_id) REFERENCES public.knowledge_versions(document_id,id) ON DELETE RESTRICT;
ALTER TABLE public.knowledge_chunks ADD COLUMN source_locator jsonb CHECK(source_locator IS NULL OR jsonb_typeof(source_locator)='object');

CREATE TABLE public.knowledge_document_scope (
 document_id bigint PRIMARY KEY REFERENCES public.knowledge_documents(id) ON DELETE RESTRICT,
 knowledge_base_id uuid NOT NULL REFERENCES public.knowledge_base(id) ON DELETE RESTRICT,
 asset_key varchar(100) NOT NULL CHECK(asset_key ~ '^[a-z0-9][a-z0-9-]{0,99}$'),
 UNIQUE(knowledge_base_id,asset_key)
);
CREATE TABLE public.knowledge_asset_metadata (
 version_id bigint PRIMARY KEY, document_id bigint NOT NULL,
 version_number int NOT NULL CHECK(version_number>0), UNIQUE(document_id,version_number),
 FOREIGN KEY(document_id,version_id) REFERENCES public.knowledge_versions(document_id,id) ON DELETE RESTRICT,
 source_file_id bigint REFERENCES public.file_asset(id) ON DELETE RESTRICT,
 source_uri varchar(600), mime_type varchar(120) NOT NULL,
 uploaded_by_admin_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 publication_status varchar(20) NOT NULL DEFAULT 'READY' CHECK(publication_status IN ('READY','PUBLISHED','RETIRED')),
 published_at timestamptz, locator_metadata jsonb NOT NULL CHECK(jsonb_typeof(locator_metadata)='object'),
 CHECK((publication_status='READY' AND published_at IS NULL) OR publication_status<>'READY')
);
CREATE INDEX ix_knowledge_asset_file ON public.knowledge_asset_metadata(source_file_id);
CREATE TABLE public.knowledge_ingestion_job (
 id uuid PRIMARY KEY, knowledge_base_id uuid NOT NULL REFERENCES public.knowledge_base(id) ON DELETE RESTRICT,
 asset_key varchar(100) NOT NULL CHECK(asset_key ~ '^[a-z0-9][a-z0-9-]{0,99}$'),
 uploader_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 source_file_id bigint NOT NULL REFERENCES public.file_asset(id) ON DELETE RESTRICT,
 request_id uuid NOT NULL, input_hash char(64) NOT NULL, UNIQUE(uploader_id,request_id),
 metadata_snapshot jsonb NOT NULL CHECK(jsonb_typeof(metadata_snapshot)='object'),
 state varchar(20) NOT NULL DEFAULT 'QUEUED' CHECK(state IN ('QUEUED','PARSING','EMBEDDING','READY','FAILED','PUBLISHED','CANCELLED')),
 produced_version_id bigint REFERENCES public.knowledge_asset_metadata(version_id) ON DELETE RESTRICT,
 error_code varchar(80), parser_profile varchar(180) NOT NULL, embedding_profile varchar(180),
 attempt_history jsonb NOT NULL DEFAULT '[]' CHECK(jsonb_typeof(attempt_history)='array'),
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, started_at timestamptz, finished_at timestamptz,
 row_version bigint NOT NULL DEFAULT 0 CHECK(row_version>=0)
);
CREATE INDEX ix_ingestion_kb_created ON public.knowledge_ingestion_job(knowledge_base_id,created_at);
CREATE INDEX ix_ingestion_version ON public.knowledge_ingestion_job(produced_version_id);
CREATE INDEX ix_ingestion_file ON public.knowledge_ingestion_job(source_file_id);

CREATE TABLE public.agent (
 id uuid PRIMARY KEY, name varchar(180) NOT NULL CHECK(length(btrim(name))>0), purpose text NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK(status IN ('DRAFT','ACTIVE','ARCHIVED')),
 allowed_roles jsonb NOT NULL DEFAULT '["STUDENT"]' CHECK(allowed_roles='["STUDENT"]'::jsonb),
 published_configuration_id uuid, created_by_admin_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 row_version bigint NOT NULL DEFAULT 0 CHECK(row_version>=0), CHECK(status<>'ACTIVE' OR published_configuration_id IS NOT NULL)
);
CREATE TABLE public.agent_configuration (
 id uuid PRIMARY KEY, agent_id uuid NOT NULL REFERENCES public.agent(id) ON DELETE RESTRICT,
 version_number int NOT NULL CHECK(version_number>0), UNIQUE(agent_id,version_number), UNIQUE(agent_id,id),
 model_key varchar(180) NOT NULL, prompt_template text NOT NULL, prompt_template_ref varchar(500) NOT NULL,
 prompt_hash char(64) NOT NULL, tool_policy jsonb NOT NULL CHECK(jsonb_typeof(tool_policy)='object'),
 memory_policy jsonb NOT NULL CHECK(jsonb_typeof(memory_policy)='object'), output_policy jsonb NOT NULL CHECK(jsonb_typeof(output_policy)='object'),
 status varchar(20) NOT NULL DEFAULT 'SAVED' CHECK(status IN ('SAVED','PUBLISHED','RETIRED')),
 created_by_admin_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, published_at timestamptz,
 content_hash char(64) NOT NULL, sealed boolean NOT NULL DEFAULT false,
 CHECK(status='SAVED' OR (sealed AND published_at IS NOT NULL))
);
ALTER TABLE public.agent ADD CONSTRAINT fk_agent_published_same_agent
 FOREIGN KEY(id,published_configuration_id) REFERENCES public.agent_configuration(agent_id,id) ON DELETE RESTRICT;
CREATE TABLE public.agent_config_kb_scope (
 configuration_id uuid NOT NULL REFERENCES public.agent_configuration(id) ON DELETE RESTRICT,
 knowledge_base_id uuid NOT NULL REFERENCES public.knowledge_base(id) ON DELETE RESTRICT,
 access_scope jsonb NOT NULL DEFAULT '{"mode":"LATEST_PUBLISHED"}' CHECK(access_scope='{"mode":"LATEST_PUBLISHED"}'::jsonb),
 PRIMARY KEY(configuration_id,knowledge_base_id)
);
CREATE INDEX ix_agent_kb_scope ON public.agent_config_kb_scope(knowledge_base_id);
CREATE TABLE public.agent_assignment (
 id uuid PRIMARY KEY, agent_id uuid NOT NULL REFERENCES public.agent(id) ON DELETE RESTRICT,
 student_user_id bigint NOT NULL REFERENCES public.student_profiles(user_id) ON DELETE RESTRICT,
 assigned_by_admin_user_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','REVOKED')),
 assigned_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, revoked_at timestamptz,
 CHECK((status='ACTIVE' AND revoked_at IS NULL) OR (status='REVOKED' AND revoked_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_agent_assignment_active ON public.agent_assignment(agent_id,student_user_id) WHERE status='ACTIVE';
CREATE INDEX ix_agent_assignment_student ON public.agent_assignment(student_user_id,status);

-- FK targets for P5 only; this migration does not invent any Student chat/history.
CREATE TABLE public.agent_session (
 id uuid PRIMARY KEY, owner_user_id bigint NOT NULL REFERENCES public.student_profiles(user_id) ON DELETE RESTRICT,
 agent_id uuid NOT NULL, configuration_id uuid NOT NULL, FOREIGN KEY(agent_id,configuration_id) REFERENCES public.agent_configuration(agent_id,id) ON DELETE RESTRICT,
 title varchar(180) NOT NULL, status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','DELETED')),
 next_sequence bigint NOT NULL DEFAULT 1 CHECK(next_sequence>=1), context_summary jsonb CHECK(context_summary IS NULL OR jsonb_typeof(context_summary)='object'),
 summary_source_refs jsonb NOT NULL DEFAULT '[]' CHECK(jsonb_typeof(summary_source_refs)='array'),
 summary_status varchar(20) NOT NULL DEFAULT 'NONE' CHECK(summary_status IN ('NONE','VALID','INVALIDATED')),
 summary_updated_at timestamptz, context_epoch bigint NOT NULL DEFAULT 0 CHECK(context_epoch>=0),
 started_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted_at timestamptz,
 row_version bigint NOT NULL DEFAULT 0 CHECK(row_version>=0), UNIQUE(id,configuration_id), UNIQUE(id,owner_user_id),
 CHECK((status='ACTIVE' AND deleted_at IS NULL) OR (status='DELETED' AND deleted_at IS NOT NULL))
);
CREATE INDEX ix_agent_session_owner ON public.agent_session(owner_user_id,started_at);
CREATE TABLE public.interaction_record (
 id uuid PRIMARY KEY, session_id uuid NOT NULL, owner_user_id bigint NOT NULL,
 FOREIGN KEY(session_id,owner_user_id) REFERENCES public.agent_session(id,owner_user_id) ON DELETE RESTRICT,
 sequence_number bigint NOT NULL CHECK(sequence_number>0), actor_type varchar(20) NOT NULL CHECK(actor_type IN ('USER','ASSISTANT')),
 content jsonb NOT NULL CHECK(jsonb_typeof(content)='object'), execution_id uuid,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted_at timestamptz,
 UNIQUE(session_id,sequence_number), UNIQUE(session_id,id), UNIQUE(id,owner_user_id)
);
CREATE TABLE public.agent_summary_source (
 target_session_id uuid NOT NULL, source_interaction_id uuid NOT NULL, owner_user_id bigint NOT NULL,
 FOREIGN KEY(target_session_id,owner_user_id) REFERENCES public.agent_session(id,owner_user_id) ON DELETE RESTRICT,
 FOREIGN KEY(source_interaction_id,owner_user_id) REFERENCES public.interaction_record(id,owner_user_id) ON DELETE RESTRICT,
 PRIMARY KEY(target_session_id,source_interaction_id)
);
CREATE INDEX ix_agent_summary_source ON public.agent_summary_source(source_interaction_id);
CREATE TABLE public.agent_execution (
 id uuid PRIMARY KEY, execution_mode varchar(24) NOT NULL CHECK(execution_mode IN ('STUDENT_CHAT','ADMIN_PREVIEW','KB_TEST')),
 task_type varchar(24) NOT NULL CHECK(task_type IN ('CHAT','INSIGHT_REPORT','PLAN_SUGGESTION','KB_QUERY')),
 initiated_by_user_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 session_id uuid, configuration_id uuid REFERENCES public.agent_configuration(id) ON DELETE RESTRICT,
 input_interaction_id uuid, knowledge_base_id uuid REFERENCES public.knowledge_base(id) ON DELETE RESTRICT,
 input_snapshot jsonb NOT NULL CHECK(jsonb_typeof(input_snapshot)='object'), request_id uuid NOT NULL, request_hash char(64) NOT NULL,
 UNIQUE(initiated_by_user_id,execution_mode,request_id), UNIQUE(id,initiated_by_user_id), UNIQUE(id,session_id),
 FOREIGN KEY(session_id,configuration_id) REFERENCES public.agent_session(id,configuration_id) ON DELETE RESTRICT,
 FOREIGN KEY(session_id,input_interaction_id) REFERENCES public.interaction_record(session_id,id) ON DELETE RESTRICT,
 status varchar(20) NOT NULL DEFAULT 'ACCEPTED' CHECK(status IN ('ACCEPTED','RUNNING','SUCCEEDED','REFUSED','FAILED','CANCELLED')),
 refusal_or_error_code varchar(80), tool_steps_and_sources jsonb NOT NULL DEFAULT '[]' CHECK(jsonb_typeof(tool_steps_and_sources)='array'),
 output_snapshot jsonb CHECK(output_snapshot IS NULL OR jsonb_typeof(output_snapshot)='object'),
 model_key varchar(180), prompt_hash char(64), configuration_hash char(64), pricing_snapshot jsonb CHECK(pricing_snapshot IS NULL OR jsonb_typeof(pricing_snapshot)='object'),
 input_tokens bigint CHECK(input_tokens>=0), output_tokens bigint CHECK(output_tokens>=0), estimated_cost numeric(18,8) CHECK(estimated_cost>=0),
 cost_status varchar(24) NOT NULL DEFAULT 'UNKNOWN' CHECK(cost_status IN ('UNKNOWN','NOT_APPLICABLE','ESTIMATED','PARTIAL')),
 latency_ms bigint CHECK(latency_ms>=0), started_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP, finished_at timestamptz, cancellation_requested_at timestamptz,
 privacy_version bigint NOT NULL CHECK(privacy_version>=0), context_epoch bigint CHECK(context_epoch>=0),
 CHECK((status IN ('ACCEPTED','RUNNING') AND finished_at IS NULL) OR (status IN ('SUCCEEDED','REFUSED','FAILED','CANCELLED') AND finished_at IS NOT NULL)),
 CHECK((execution_mode='STUDENT_CHAT' AND session_id IS NOT NULL AND configuration_id IS NOT NULL AND input_interaction_id IS NOT NULL AND task_type IN ('CHAT','INSIGHT_REPORT','PLAN_SUGGESTION') AND knowledge_base_id IS NULL)
 OR (execution_mode='ADMIN_PREVIEW' AND configuration_id IS NOT NULL AND session_id IS NULL AND input_interaction_id IS NULL AND knowledge_base_id IS NULL AND task_type='CHAT')
 OR (execution_mode='KB_TEST' AND knowledge_base_id IS NOT NULL AND session_id IS NULL AND input_interaction_id IS NULL AND task_type='KB_QUERY')),
 CHECK(cost_status<>'NOT_APPLICABLE' OR (input_tokens IS NULL AND output_tokens IS NULL AND estimated_cost IS NULL)),
 CHECK(cost_status<>'ESTIMATED' OR (input_tokens IS NOT NULL AND output_tokens IS NOT NULL AND estimated_cost IS NOT NULL))
 ,CHECK(status<>'SUCCEEDED' OR output_snapshot IS NOT NULL)
);
ALTER TABLE public.interaction_record ADD CONSTRAINT fk_interaction_execution FOREIGN KEY(execution_id) REFERENCES public.agent_execution(id) ON DELETE RESTRICT;
ALTER TABLE public.interaction_record ADD CONSTRAINT fk_interaction_execution_session FOREIGN KEY(execution_id,session_id) REFERENCES public.agent_execution(id,session_id) ON DELETE RESTRICT;
CREATE UNIQUE INDEX uq_agent_session_active_execution ON public.agent_execution(session_id) WHERE execution_mode='STUDENT_CHAT' AND status IN ('ACCEPTED','RUNNING');
CREATE INDEX ix_agent_execution_started_mode ON public.agent_execution(started_at,execution_mode,status);
CREATE INDEX ix_agent_execution_initiator ON public.agent_execution(initiated_by_user_id,started_at);
CREATE INDEX ix_agent_execution_session ON public.agent_execution(session_id,started_at);
CREATE INDEX ix_agent_execution_config ON public.agent_execution(configuration_id);
CREATE INDEX ix_agent_execution_kb ON public.agent_execution(knowledge_base_id);

CREATE FUNCTION public.require_platform_admin(uid bigint) RETURNS void LANGUAGE plpgsql AS $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM public.app_users WHERE id=uid AND role='ADMINISTRATOR' AND status='ACTIVE') THEN RAISE EXCEPTION 'ACTIVE_ADMIN_REQUIRED'; END IF;
END $$;
CREATE FUNCTION public.guard_kb_scope() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'KB_SCOPE_IMMUTABLE'; END IF;
 PERFORM 1 FROM public.knowledge_documents WHERE id=NEW.document_id AND status='REVOKED' AND active_version_id IS NULL FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'REVIEWED_SCOPE_ADOPTION_REQUIRED'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_kb_scope BEFORE INSERT OR UPDATE OR DELETE ON public.knowledge_document_scope FOR EACH ROW EXECUTE FUNCTION public.guard_kb_scope();

CREATE FUNCTION public.guard_scoped_knowledge_content() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE vid bigint; did bigint;
BEGIN
 IF TG_OP='UPDATE' THEN
  IF TG_TABLE_NAME='knowledge_versions' THEN
   IF NEW.document_id<>OLD.document_id THEN RAISE EXCEPTION 'KNOWLEDGE_PARENT_IMMUTABLE'; END IF;
  ELSIF TG_TABLE_NAME='knowledge_chunks' THEN
   IF NEW.version_id<>OLD.version_id THEN RAISE EXCEPTION 'KNOWLEDGE_PARENT_IMMUTABLE'; END IF;
  ELSE
   IF NEW.version_id<>OLD.version_id OR NEW.document_id<>OLD.document_id THEN RAISE EXCEPTION 'KNOWLEDGE_PARENT_IMMUTABLE'; END IF;
  END IF;
 END IF;
 IF TG_TABLE_NAME='knowledge_versions' THEN vid:=CASE WHEN TG_OP='DELETE' THEN OLD.id ELSE NEW.id END;
 ELSIF TG_TABLE_NAME='knowledge_asset_metadata' THEN vid:=CASE WHEN TG_OP='DELETE' THEN OLD.version_id ELSE NEW.version_id END;
 ELSE vid:=CASE WHEN TG_OP='DELETE' THEN OLD.version_id ELSE NEW.version_id END; END IF;
 SELECT document_id INTO did FROM public.knowledge_versions WHERE id=vid;
 IF EXISTS(SELECT 1 FROM public.knowledge_document_scope WHERE document_id=did) THEN
  PERFORM 1 FROM public.knowledge_documents WHERE id=did FOR UPDATE;
  IF TG_TABLE_NAME='knowledge_asset_metadata' THEN
   IF TG_OP='DELETE' THEN RAISE EXCEPTION 'KNOWLEDGE_VERSION_IMMUTABLE'; END IF;
   IF TG_OP='INSERT' THEN
    IF NEW.document_id<>did OR NEW.publication_status<>'READY' THEN RAISE EXCEPTION 'READY_SOURCE_REQUIRED'; END IF;
    PERFORM public.require_platform_admin(NEW.uploaded_by_admin_id);
   ELSE
    IF (to_jsonb(NEW)-'publication_status'-'published_at') IS DISTINCT FROM (to_jsonb(OLD)-'publication_status'-'published_at') THEN RAISE EXCEPTION 'KNOWLEDGE_VERSION_IMMUTABLE'; END IF;
    IF OLD.publication_status='RETIRED' AND NEW.publication_status<>'RETIRED' THEN RAISE EXCEPTION 'RETIRED_SOURCE_IMMUTABLE'; END IF;
    IF OLD.published_at IS NOT NULL AND NEW.published_at IS DISTINCT FROM OLD.published_at THEN RAISE EXCEPTION 'PUBLICATION_TIME_IMMUTABLE'; END IF;
    IF NEW.publication_status='PUBLISHED' AND (NOT EXISTS(SELECT 1 FROM public.knowledge_chunks WHERE version_id=vid) OR EXISTS(SELECT 1 FROM public.knowledge_chunks WHERE version_id=vid AND source_locator IS NULL)) THEN RAISE EXCEPTION 'COMPLETE_SOURCE_CHUNKS_REQUIRED'; END IF;
   END IF;
  ELSIF EXISTS(SELECT 1 FROM public.knowledge_asset_metadata WHERE version_id=vid) THEN RAISE EXCEPTION 'KNOWLEDGE_VERSION_IMMUTABLE'; END IF;
 END IF;
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER guard_scoped_knowledge_versions BEFORE UPDATE OR DELETE ON public.knowledge_versions FOR EACH ROW EXECUTE FUNCTION public.guard_scoped_knowledge_content();
CREATE TRIGGER guard_scoped_knowledge_chunks BEFORE INSERT OR UPDATE OR DELETE ON public.knowledge_chunks FOR EACH ROW EXECUTE FUNCTION public.guard_scoped_knowledge_content();
CREATE TRIGGER guard_scoped_knowledge_metadata BEFORE INSERT OR UPDATE OR DELETE ON public.knowledge_asset_metadata FOR EACH ROW EXECUTE FUNCTION public.guard_scoped_knowledge_content();
CREATE FUNCTION public.guard_scoped_knowledge_pointer() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF EXISTS(SELECT 1 FROM public.knowledge_document_scope WHERE document_id=NEW.id) AND NEW.status='PUBLISHED'
 AND NOT EXISTS(SELECT 1 FROM public.knowledge_asset_metadata WHERE document_id=NEW.id AND version_id=NEW.active_version_id AND publication_status='PUBLISHED') THEN RAISE EXCEPTION 'PUBLISHED_SCOPED_VERSION_REQUIRED'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_scoped_knowledge_pointer BEFORE UPDATE ON public.knowledge_documents FOR EACH ROW EXECUTE FUNCTION public.guard_scoped_knowledge_pointer();

CREATE FUNCTION public.guard_agent_configuration() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN PERFORM public.require_platform_admin(NEW.created_by_admin_id); RETURN NEW; END IF;
 IF TG_OP='DELETE' OR (to_jsonb(NEW)-'status'-'published_at'-'sealed') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'published_at'-'sealed') THEN RAISE EXCEPTION 'AGENT_CONFIGURATION_IMMUTABLE'; END IF;
 IF OLD.sealed AND NOT NEW.sealed THEN RAISE EXCEPTION 'CONFIGURATION_SEALED'; END IF;
 IF OLD.status='RETIRED' AND NEW.status<>'RETIRED' THEN RAISE EXCEPTION 'RETIRED_CONFIGURATION_IMMUTABLE'; END IF;
 IF OLD.status='PUBLISHED' AND NEW.status NOT IN ('PUBLISHED','RETIRED') THEN RAISE EXCEPTION 'CONFIGURATION_STATUS_INVALID'; END IF;
 IF OLD.published_at IS NOT NULL AND NEW.published_at IS DISTINCT FROM OLD.published_at THEN RAISE EXCEPTION 'PUBLICATION_TIME_IMMUTABLE'; END IF;
 IF NEW.status='PUBLISHED' AND OLD.status='SAVED' AND NOT EXISTS(
  SELECT 1 FROM public.agent_execution WHERE configuration_id=NEW.id AND configuration_hash=NEW.content_hash AND execution_mode='ADMIN_PREVIEW' AND status='SUCCEEDED'
 ) THEN RAISE EXCEPTION 'EXACT_CONFIGURATION_PREVIEW_REQUIRED'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_agent_configuration BEFORE INSERT OR UPDATE OR DELETE ON public.agent_configuration FOR EACH ROW EXECUTE FUNCTION public.guard_agent_configuration();
CREATE FUNCTION public.guard_agent_config_grants() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE cid uuid; locked boolean;
BEGIN
 cid:=CASE WHEN TG_OP='DELETE' THEN OLD.configuration_id ELSE NEW.configuration_id END;
 SELECT sealed INTO locked FROM public.agent_configuration WHERE id=cid FOR UPDATE;
 IF locked IS DISTINCT FROM false THEN RAISE EXCEPTION 'CONFIGURATION_GRANTS_IMMUTABLE'; END IF;
 IF TG_OP='UPDATE' THEN RAISE EXCEPTION 'CONFIGURATION_GRANTS_IMMUTABLE'; END IF;
 IF TG_OP='INSERT' AND NOT EXISTS(SELECT 1 FROM public.knowledge_base WHERE id=NEW.knowledge_base_id AND scope_type='PLATFORM' AND status='ACTIVE') THEN RAISE EXCEPTION 'ACTIVE_PLATFORM_KB_REQUIRED'; END IF;
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER guard_agent_config_grants BEFORE INSERT OR UPDATE OR DELETE ON public.agent_config_kb_scope FOR EACH ROW EXECUTE FUNCTION public.guard_agent_config_grants();
CREATE FUNCTION public.guard_agent_published_pointer() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.published_configuration_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.agent_configuration WHERE id=NEW.published_configuration_id AND agent_id=NEW.id AND status='PUBLISHED' AND sealed) THEN RAISE EXCEPTION 'PUBLISHED_SAME_AGENT_CONFIGURATION_REQUIRED'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_agent_published_pointer BEFORE INSERT OR UPDATE ON public.agent FOR EACH ROW EXECUTE FUNCTION public.guard_agent_published_pointer();
CREATE FUNCTION public.guard_agent_assignment() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ASSIGNMENT_HISTORY_IMMUTABLE'; END IF;
 IF TG_OP='INSERT' THEN
  PERFORM public.require_platform_admin(NEW.assigned_by_admin_user_id);
  PERFORM 1 FROM public.agent WHERE id=NEW.agent_id AND status='ACTIVE' FOR UPDATE;
  IF NOT FOUND OR NOT EXISTS(SELECT 1 FROM public.app_users WHERE id=NEW.student_user_id AND status='ACTIVE' AND role='STUDENT') THEN RAISE EXCEPTION 'ACTIVE_AGENT_STUDENT_REQUIRED'; END IF;
 ELSE
  IF (to_jsonb(NEW)-'status'-'revoked_at') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'revoked_at') OR (OLD.status='REVOKED' AND NEW.status<>'REVOKED') THEN RAISE EXCEPTION 'ASSIGNMENT_HISTORY_IMMUTABLE'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_agent_assignment BEFORE INSERT OR UPDATE OR DELETE ON public.agent_assignment FOR EACH ROW EXECUTE FUNCTION public.guard_agent_assignment();

CREATE FUNCTION public.guard_agent_session() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  IF NOT EXISTS(SELECT 1 FROM public.agent a JOIN public.agent_configuration c ON c.id=NEW.configuration_id AND c.agent_id=a.id JOIN public.agent_assignment x ON x.agent_id=a.id AND x.student_user_id=NEW.owner_user_id AND x.status='ACTIVE' JOIN public.app_users u ON u.id=x.student_user_id AND u.status='ACTIVE' AND u.role='STUDENT' WHERE a.id=NEW.agent_id AND a.status='ACTIVE' AND c.status='PUBLISHED' AND c.sealed) THEN RAISE EXCEPTION 'ASSIGNED_PUBLISHED_AGENT_REQUIRED'; END IF;
 ELSIF TG_OP='DELETE' OR NEW.id<>OLD.id OR NEW.owner_user_id<>OLD.owner_user_id OR NEW.agent_id<>OLD.agent_id OR NEW.configuration_id<>OLD.configuration_id THEN RAISE EXCEPTION 'SESSION_OWNER_CONFIG_IMMUTABLE'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_agent_session BEFORE INSERT OR UPDATE OR DELETE ON public.agent_session FOR EACH ROW EXECUTE FUNCTION public.guard_agent_session();
CREATE FUNCTION public.guard_agent_execution() RETURNS trigger LANGUAGE plpgsql AS $$
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
  IF NOT EXISTS(SELECT 1 FROM public.agent_session s JOIN public.agent a ON a.id=s.agent_id AND a.status='ACTIVE' JOIN public.agent_assignment x ON x.agent_id=a.id AND x.student_user_id=owner_id AND x.status='ACTIVE' WHERE s.id=NEW.session_id) THEN RAISE EXCEPTION 'ACTIVE_ASSIGNMENT_REQUIRED'; END IF;
 END IF;
 IF NEW.execution_mode='KB_TEST' AND NEW.configuration_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.agent_config_kb_scope WHERE configuration_id=NEW.configuration_id AND knowledge_base_id=NEW.knowledge_base_id) THEN RAISE EXCEPTION 'CONFIGURATION_KB_GRANT_REQUIRED'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_agent_execution BEFORE INSERT OR UPDATE OR DELETE ON public.agent_execution FOR EACH ROW EXECUTE FUNCTION public.guard_agent_execution();

CREATE FUNCTION public.guard_platform_creator() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 PERFORM public.require_platform_admin(NEW.created_by_admin_id); RETURN NEW;
END $$;
CREATE TRIGGER guard_kb_creator BEFORE INSERT ON public.knowledge_base FOR EACH ROW EXECUTE FUNCTION public.guard_platform_creator();
CREATE TRIGGER guard_agent_creator BEFORE INSERT ON public.agent FOR EACH ROW EXECUTE FUNCTION public.guard_platform_creator();
CREATE FUNCTION public.guard_ingestion_job() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  PERFORM public.require_platform_admin(NEW.uploader_id);
  IF NOT EXISTS(SELECT 1 FROM public.file_asset WHERE id=NEW.source_file_id AND owner_user_id=NEW.uploader_id AND state='READY') THEN RAISE EXCEPTION 'OWNED_READY_SOURCE_REQUIRED'; END IF;
 ELSE
  IF (to_jsonb(NEW)-'state'-'produced_version_id'-'error_code'-'embedding_profile'-'attempt_history'-'started_at'-'finished_at'-'row_version') IS DISTINCT FROM (to_jsonb(OLD)-'state'-'produced_version_id'-'error_code'-'embedding_profile'-'attempt_history'-'started_at'-'finished_at'-'row_version') THEN RAISE EXCEPTION 'INGESTION_INPUT_IMMUTABLE'; END IF;
 END IF;
 IF NEW.produced_version_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.knowledge_asset_metadata m JOIN public.knowledge_document_scope s ON s.document_id=m.document_id WHERE m.version_id=NEW.produced_version_id AND s.knowledge_base_id=NEW.knowledge_base_id AND s.asset_key=NEW.asset_key) THEN RAISE EXCEPTION 'INGESTION_VERSION_SCOPE_MISMATCH'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_ingestion_job BEFORE INSERT OR UPDATE ON public.knowledge_ingestion_job FOR EACH ROW EXECUTE FUNCTION public.guard_ingestion_job();
