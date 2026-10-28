-- Natural-language commands reuse the same immutable execution, owned input and validated artifact.
-- No backfill, table change, automatic assignment or plan persistence.
CREATE OR REPLACE FUNCTION public.guard_insight_report() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP<>'INSERT' THEN
  IF TG_OP='DELETE' OR (to_jsonb(NEW)-'status'-'deleted_at') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'deleted_at') OR OLD.status='DELETED' THEN RAISE EXCEPTION 'INSIGHT_REPORT_IMMUTABLE'; END IF;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM public.agent_execution e JOIN public.agent_session s ON s.id=e.session_id WHERE e.id=NEW.source_execution_id AND e.initiated_by_user_id=NEW.owner_user_id AND e.input_interaction_id=NEW.requested_interaction_id AND e.execution_mode='STUDENT_CHAT' AND ((e.task_type='INSIGHT_REPORT' AND e.input_snapshot->>'explicitRequest'='true') OR (e.task_type='CHAT' AND e.output_snapshot->>'requestedAction'='INSIGHT_REPORT')) AND e.status='SUCCEEDED' AND s.status='ACTIVE' AND e.output_snapshot->'report'=NEW.structured_insights AND NEW.structured_insights->>'schemaVersion'=NEW.schema_version AND NEW.structured_insights->'evidenceRefs'=NEW.evidence_refs) THEN RAISE EXCEPTION 'EXPLICIT_VALID_OWNED_REPORT_EXECUTION_REQUIRED'; END IF;
 END IF; RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION public.guard_confirmed_plan_source() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='UPDATE' AND OLD.source_execution_id IS NOT NULL THEN
  IF NEW.source_execution_id IS DISTINCT FROM OLD.source_execution_id OR NEW.confirmation_request_id IS DISTINCT FROM OLD.confirmation_request_id OR NEW.confirmation_hash IS DISTINCT FROM OLD.confirmation_hash OR NEW.title<>OLD.title OR NEW.steps<>OLD.steps OR NEW.student_id<>OLD.student_id OR NEW.confirmed_at<>OLD.confirmed_at THEN RAISE EXCEPTION 'CONFIRMED_PLAN_SOURCE_IMMUTABLE'; END IF;
  RETURN NEW;
 END IF;
 IF NEW.source_execution_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.agent_execution e JOIN public.agent_session s ON s.id=e.session_id WHERE e.id=NEW.source_execution_id AND e.initiated_by_user_id=NEW.student_id AND e.execution_mode='STUDENT_CHAT' AND (e.task_type='PLAN_SUGGESTION' OR (e.task_type='CHAT' AND e.output_snapshot->>'requestedAction'='PLAN_SUGGESTION')) AND e.status='SUCCEEDED' AND s.status='ACTIVE' AND e.output_snapshot->>'planContentHash'=NEW.confirmation_hash AND e.output_snapshot->'planSuggestion'->>'title'=NEW.title AND e.output_snapshot->'planSuggestion'->'steps'=NEW.steps) THEN RAISE EXCEPTION 'EXPLICIT_VALID_OWNED_PLAN_EXECUTION_REQUIRED'; END IF;
 RETURN NEW;
END $$;
