-- UC-05 approved demo FILE conventions. This migration contains policy metadata only.
-- It deliberately does not seed third-party scenario content. Missing scenarios on a
-- clean database remain absent; existing reviewed scenarios receive a new version.
CREATE TEMP TABLE uc05_file_policy (
    scenario_slug varchar(120) NOT NULL,
    task_position integer NOT NULL,
    allowed_mime_types jsonb NOT NULL,
    PRIMARY KEY (scenario_slug, task_position)
) ON COMMIT DROP;

INSERT INTO uc05_file_policy VALUES
    ('hpe-software-engineering', 2, '["application/zip"]'),
    ('hpe-software-engineering', 3, '["application/zip"]'),
    ('hpe-software-engineering', 4, '["text/plain"]'),
    ('hpe-presales', 1, '["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]'),
    ('hpe-presales', 3, '["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]'),
    ('hpe-presales', 4, '["application/vnd.openxmlformats-officedocument.presentationml.presentation"]'),
    ('hpe-digital-sales', 1, '["application/vnd.openxmlformats-officedocument.presentationml.presentation"]'),
    ('hpe-digital-sales', 3, '["application/vnd.openxmlformats-officedocument.presentationml.presentation"]'),
    ('datacom-software-development', 2, '["application/zip"]'),
    ('datacom-introduction-to-cloud', 1, '["application/pdf"]'),
    ('datacom-automation-ai-accelerator', 2, '["application/pdf","image/png"]'),
    ('datacom-cyber-security-operations', 1, '["application/pdf"]'),
    ('datacom-cyber-security-operations', 2, '["application/pdf","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"]');

DO $$
DECLARE
    root record;
    source_version public.scenario_version%ROWTYPE;
    new_version_id bigint;
    old_task record;
    new_task_id bigint;
    configured_count integer;
    expected_count integer;
BEGIN
    FOR root IN
        SELECT s.id, s.slug, s.current_version_id
        FROM public.scenario s
        WHERE s.slug IN (SELECT DISTINCT scenario_slug FROM uc05_file_policy)
        ORDER BY s.id
        FOR UPDATE
    LOOP
        SELECT * INTO STRICT source_version
        FROM public.scenario_version
        WHERE id = root.current_version_id AND scenario_id = root.id AND status = 'PUBLISHED';

        -- A repeated manual application of the SQL remains harmless. Flyway itself
        -- also guarantees one application per schema history.
        IF source_version.provenance @> '{"uc05FilePolicyVersion":"demo-13/1"}'::jsonb THEN
            CONTINUE;
        END IF;

        SELECT count(*) INTO expected_count
        FROM uc05_file_policy WHERE scenario_slug = root.slug;

        SELECT count(*) INTO configured_count
        FROM uc05_file_policy p
        JOIN public.simulation_task t
          ON t.scenario_version_id = source_version.id
         AND t.position = p.task_position
        JOIN public.task_deliverable_definition d
          ON d.task_id = t.id AND d.format = 'FILE'
        WHERE p.scenario_slug = root.slug;

        IF configured_count <> expected_count THEN
            RAISE EXCEPTION 'UC05_FILE_POLICY_SOURCE_MISMATCH: slug %, expected %, found %',
                root.slug, expected_count, configured_count;
        END IF;

        INSERT INTO public.scenario_version(
            scenario_id, version_number, status, title, target_role, career_track,
            summary, introduction, learning_objectives, difficulty, estimated_minutes,
            translations, provenance, card_image_url, hero_image_url,
            company_logo_url, overview_video_url)
        VALUES (
            root.id,
            (SELECT max(version_number) + 1 FROM public.scenario_version WHERE scenario_id = root.id),
            'DRAFT', source_version.title, source_version.target_role,
            source_version.career_track, source_version.summary, source_version.introduction,
            source_version.learning_objectives, source_version.difficulty,
            source_version.estimated_minutes, source_version.translations,
            source_version.provenance || '{"uc05FilePolicyVersion":"demo-13/1"}'::jsonb,
            source_version.card_image_url, source_version.hero_image_url,
            source_version.company_logo_url, source_version.overview_video_url)
        RETURNING id INTO new_version_id;

        INSERT INTO public.scenario_learning_policy(
            scenario_version_id, policy_version, progression_mode, scoring_schema_version)
        SELECT new_version_id, policy_version, progression_mode, scoring_schema_version
        FROM public.scenario_learning_policy
        WHERE scenario_version_id = source_version.id;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'UC05_FILE_POLICY_ORDERED_POLICY_REQUIRED: slug %', root.slug;
        END IF;

        FOR old_task IN
            SELECT * FROM public.simulation_task
            WHERE scenario_version_id = source_version.id
            ORDER BY position
        LOOP
            INSERT INTO public.simulation_task(
                scenario_version_id, position, title, workplace_context,
                instructions, required, translations)
            VALUES (
                new_version_id, old_task.position, old_task.title,
                old_task.workplace_context, old_task.instructions,
                old_task.required, old_task.translations)
            RETURNING id INTO new_task_id;

            INSERT INTO public.task_deliverable_definition(
                task_id, position, name, format, required, validation_config)
            SELECT new_task_id, d.position, d.name, d.format, d.required,
                   CASE WHEN d.format = 'FILE' AND p.task_position IS NOT NULL
                        THEN jsonb_build_object(
                            'schemaVersion', 1,
                            'maxBytes', 10485760,
                            'allowedMimeTypes', p.allowed_mime_types)
                        ELSE d.validation_config END
            FROM public.task_deliverable_definition d
            LEFT JOIN uc05_file_policy p
              ON p.scenario_slug = root.slug
             AND p.task_position = old_task.position
            WHERE d.task_id = old_task.id
            ORDER BY d.position;

            INSERT INTO public.task_resource(
                task_id, position, title, file_asset_id, external_url, release_policy)
            SELECT new_task_id, position, title, file_asset_id, external_url, release_policy
            FROM public.task_resource WHERE task_id = old_task.id ORDER BY position;

            INSERT INTO public.model_answer(task_id, content, translations, release_policy)
            SELECT new_task_id, content, translations, release_policy
            FROM public.model_answer WHERE task_id = old_task.id;

            INSERT INTO public.task_skill(task_id, skill_id, skill_name_snapshot)
            SELECT new_task_id, skill_id, skill_name_snapshot
            FROM public.task_skill WHERE task_id = old_task.id;

            INSERT INTO public.task_learning_policy(task_id, task_type)
            SELECT new_task_id, task_type
            FROM public.task_learning_policy WHERE task_id = old_task.id;

            INSERT INTO public.task_rubric(task_id, version, evaluator_key, quiz_scoring_config)
            SELECT new_task_id, version, evaluator_key, quiz_scoring_config
            FROM public.task_rubric WHERE task_id = old_task.id;
        END LOOP;

        UPDATE public.scenario_version
        SET status = 'PUBLISHED',
            content_hash = md5(coalesce(source_version.content_hash, '') || ':uc05:demo-13/1')
                         || md5(':uc05:demo-13/1:' || coalesce(source_version.content_hash, '')),
            published_at = CURRENT_TIMESTAMP,
            row_version = row_version + 1
        WHERE id = new_version_id;

        UPDATE public.scenario
        SET current_version_id = new_version_id,
            availability_status = 'AVAILABLE',
            row_version = row_version + 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = root.id;
    END LOOP;
END $$;
