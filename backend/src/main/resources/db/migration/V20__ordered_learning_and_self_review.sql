-- Missing sidecars remain historical semantics; no historical score/review/actor is fabricated.
CREATE TABLE public.scenario_learning_policy (
 scenario_version_id bigint PRIMARY KEY REFERENCES public.scenario_version(id) ON DELETE RESTRICT,
 policy_version varchar(40) NOT NULL CHECK(policy_version='ordered-review/1'),
 progression_mode varchar(40) NOT NULL CHECK(progression_mode='ORDERED_SELF_REVIEW'),
 scoring_schema_version varchar(40) NOT NULL CHECK(scoring_schema_version='1')
);
CREATE TABLE public.task_learning_policy (
 task_id bigint PRIMARY KEY REFERENCES public.simulation_task(id) ON DELETE RESTRICT,
 task_type varchar(32) NOT NULL CHECK(task_type IN ('QUIZ','TEXT','PRACTICAL'))
);
CREATE TABLE public.task_rubric (
 task_id bigint PRIMARY KEY REFERENCES public.task_learning_policy(task_id) ON DELETE RESTRICT,
 version int NOT NULL CHECK(version>0),
 evaluator_key varchar(80) NOT NULL CHECK(evaluator_key='quiz-exact/1'),
 quiz_scoring_config jsonb NOT NULL CHECK(jsonb_typeof(quiz_scoring_config)='object')
);
CREATE TABLE public.task_quiz_result (
 submission_id bigint PRIMARY KEY,
 task_id bigint NOT NULL REFERENCES public.task_rubric(task_id) ON DELETE RESTRICT,
 rubric_version int NOT NULL CHECK(rubric_version>0),
 evaluator_key varchar(80) NOT NULL,
 score_data jsonb NOT NULL CHECK(jsonb_typeof(score_data)='object'),
 evaluated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(submission_id,task_id) REFERENCES public.task_submission(id,task_id) ON DELETE RESTRICT
);
CREATE INDEX ix_task_quiz_result_task ON public.task_quiz_result(task_id);
CREATE TABLE public.task_self_review (
 submission_id bigint PRIMARY KEY REFERENCES public.task_submission(id) ON DELETE RESTRICT,
 reviewed_by_user_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 reviewed_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_task_self_review_actor ON public.task_self_review(reviewed_by_user_id);
CREATE TABLE public.task_submission_actor (
 submission_id bigint PRIMARY KEY REFERENCES public.task_submission(id) ON DELETE RESTRICT,
 submitted_by_user_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 recorded_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_task_submission_actor_user ON public.task_submission_actor(submitted_by_user_id);

CREATE FUNCTION public.guard_learning_content() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE vid bigint; st text; tid bigint;
BEGIN
 IF TG_OP='UPDATE' THEN
  IF TG_TABLE_NAME='scenario_learning_policy' THEN
   IF NEW.scenario_version_id<>OLD.scenario_version_id THEN RAISE EXCEPTION 'LEARNING_PARENT_IMMUTABLE'; END IF;
  ELSE
   IF NEW.task_id<>OLD.task_id THEN RAISE EXCEPTION 'LEARNING_PARENT_IMMUTABLE'; END IF;
  END IF;
 END IF;
 IF TG_TABLE_NAME='scenario_learning_policy' THEN
  vid := CASE WHEN TG_OP='DELETE' THEN OLD.scenario_version_id ELSE NEW.scenario_version_id END;
 ELSE
  tid := CASE WHEN TG_OP='DELETE' THEN OLD.task_id ELSE NEW.task_id END;
  SELECT scenario_version_id INTO vid FROM public.simulation_task WHERE id=tid;
 END IF;
 SELECT status INTO st FROM public.scenario_version WHERE id=vid FOR UPDATE;
 IF st IS DISTINCT FROM 'DRAFT' THEN RAISE EXCEPTION 'LEARNING_CONTENT_IMMUTABLE'; END IF;
 IF TG_OP<>'DELETE' AND TG_TABLE_NAME<>'scenario_learning_policy' AND NOT EXISTS(SELECT 1 FROM public.scenario_learning_policy WHERE scenario_version_id=vid) THEN RAISE EXCEPTION 'LEARNING_POLICY_REQUIRED'; END IF;
 IF TG_OP<>'DELETE' AND TG_TABLE_NAME='task_rubric' AND NOT EXISTS(SELECT 1 FROM public.task_learning_policy WHERE task_id=tid AND task_type='QUIZ') THEN RAISE EXCEPTION 'QUIZ_RUBRIC_ONLY'; END IF;
 IF TG_TABLE_NAME='task_learning_policy' AND TG_OP<>'DELETE' THEN
  IF NEW.task_type<>'QUIZ' AND EXISTS(SELECT 1 FROM public.task_rubric WHERE task_id=tid) THEN RAISE EXCEPTION 'QUIZ_RUBRIC_ONLY'; END IF;
 END IF;
 RETURN CASE WHEN TG_OP='DELETE' THEN OLD ELSE NEW END;
END $$;
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['scenario_learning_policy','task_learning_policy','task_rubric'] LOOP
  EXECUTE format('CREATE TRIGGER guard_learning_content BEFORE INSERT OR UPDATE OR DELETE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.guard_learning_content()',t);
 END LOOP;
END $$;

CREATE FUNCTION public.guard_learning_publication() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF NEW.status='PUBLISHED' AND OLD.status='DRAFT' THEN
  IF NOT EXISTS(SELECT 1 FROM public.scenario_learning_policy WHERE scenario_version_id=NEW.id)
   OR NOT EXISTS(SELECT 1 FROM public.simulation_task WHERE scenario_version_id=NEW.id)
   OR EXISTS(SELECT 1 FROM public.simulation_task t LEFT JOIN public.task_learning_policy p ON p.task_id=t.id WHERE t.scenario_version_id=NEW.id AND (NOT t.required OR p.task_id IS NULL))
  THEN RAISE EXCEPTION 'NEW_PUBLICATION_REQUIRES_ORDERED_ALL_REQUIRED_POLICY'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_learning_publication BEFORE UPDATE ON public.scenario_version FOR EACH ROW EXECUTE FUNCTION public.guard_learning_publication();

CREATE FUNCTION public.guard_learning_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE owner_id bigint; st text; tid bigint; rv int; ek text;
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'LEARNING_EVIDENCE_IMMUTABLE'; END IF;
 SELECT e.student_id,s.status,s.task_id INTO owner_id,st,tid FROM public.task_submission s JOIN public.simulation_enrollment e ON e.id=s.enrollment_id WHERE s.id=NEW.submission_id FOR UPDATE OF e,s;
 IF st IS DISTINCT FROM 'FINAL' THEN RAISE EXCEPTION 'FINAL_SUBMISSION_REQUIRED'; END IF;
 IF TG_TABLE_NAME='task_self_review' THEN
  IF NEW.reviewed_by_user_id<>owner_id THEN RAISE EXCEPTION 'REVIEW_OWNER_REQUIRED'; END IF;
 END IF;
 IF TG_TABLE_NAME='task_submission_actor' THEN
  IF NEW.submitted_by_user_id<>owner_id THEN RAISE EXCEPTION 'SUBMISSION_ACTOR_OWNER_REQUIRED'; END IF;
 END IF;
 IF TG_TABLE_NAME='task_quiz_result' THEN
  SELECT version,evaluator_key INTO rv,ek FROM public.task_rubric WHERE task_id=tid;
  IF NEW.task_id<>tid OR NEW.rubric_version IS DISTINCT FROM rv OR NEW.evaluator_key IS DISTINCT FROM ek THEN RAISE EXCEPTION 'QUIZ_RESULT_RUBRIC_MISMATCH'; END IF;
 END IF;
 RETURN NEW;
END $$;
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['task_quiz_result','task_self_review','task_submission_actor'] LOOP
  EXECUTE format('CREATE TRIGGER guard_learning_evidence BEFORE INSERT OR UPDATE OR DELETE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.guard_learning_evidence()',t);
 END LOOP;
END $$;

CREATE INDEX ix_scenario_version_catalog_filters ON public.scenario_version(career_track,difficulty,estimated_minutes,id) WHERE status='PUBLISHED';
CREATE INDEX ix_simulation_enrollment_version_status ON public.simulation_enrollment(scenario_version_id,status,id);
