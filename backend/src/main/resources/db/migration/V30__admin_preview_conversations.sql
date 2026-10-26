CREATE TABLE public.admin_preview_conversation (
 id uuid PRIMARY KEY,
 owner_admin_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 agent_id uuid NOT NULL, configuration_id uuid NOT NULL,
 FOREIGN KEY(agent_id,configuration_id) REFERENCES public.agent_configuration(agent_id,id) ON DELETE RESTRICT,
 request_id uuid NOT NULL, status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','DELETED')),
 context_epoch bigint NOT NULL DEFAULT 0 CHECK(context_epoch>=0),
 next_sequence bigint NOT NULL DEFAULT 1 CHECK(next_sequence>0),
 row_version bigint NOT NULL DEFAULT 0 CHECK(row_version>=0),
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(owner_admin_id,request_id), UNIQUE(id,owner_admin_id,configuration_id)
);
CREATE INDEX ix_admin_preview_owner ON public.admin_preview_conversation(owner_admin_id,agent_id,created_at DESC,id);
CREATE TABLE public.admin_preview_turn (
 conversation_id uuid NOT NULL REFERENCES public.admin_preview_conversation(id) ON DELETE RESTRICT,
 context_epoch bigint NOT NULL CHECK(context_epoch>=0), sequence_number bigint NOT NULL CHECK(sequence_number>0),
 execution_id uuid NOT NULL UNIQUE REFERENCES public.agent_execution(id) ON DELETE RESTRICT,
 PRIMARY KEY(conversation_id,context_epoch,sequence_number)
);
CREATE FUNCTION public.guard_admin_preview_conversation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP='INSERT' THEN
  PERFORM public.require_platform_admin(NEW.owner_admin_id);
  IF NOT EXISTS(SELECT 1 FROM public.agent_configuration WHERE id=NEW.configuration_id AND agent_id=NEW.agent_id AND sealed AND status<>'RETIRED') THEN RAISE EXCEPTION 'SAVED_PREVIEW_CONFIGURATION_REQUIRED'; END IF;
 ELSE
  IF TG_OP='DELETE' OR (to_jsonb(NEW)-'status'-'context_epoch'-'next_sequence'-'row_version'-'updated_at') IS DISTINCT FROM (to_jsonb(OLD)-'status'-'context_epoch'-'next_sequence'-'row_version'-'updated_at') OR OLD.status='DELETED' OR NEW.context_epoch<OLD.context_epoch OR NEW.row_version<=OLD.row_version THEN RAISE EXCEPTION 'PREVIEW_IDENTITY_LIFECYCLE_INVALID'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_admin_preview_conversation BEFORE INSERT OR UPDATE OR DELETE ON public.admin_preview_conversation FOR EACH ROW EXECUTE FUNCTION public.guard_admin_preview_conversation();
CREATE FUNCTION public.guard_admin_preview_turn() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'PREVIEW_TURN_AUDIT_IMMUTABLE'; END IF;
 PERFORM 1 FROM public.admin_preview_conversation c JOIN public.agent_execution e ON e.id=NEW.execution_id
 WHERE c.id=NEW.conversation_id AND c.status='ACTIVE' AND c.context_epoch=NEW.context_epoch
 AND e.initiated_by_user_id=c.owner_admin_id AND e.configuration_id=c.configuration_id
 AND e.execution_mode='ADMIN_PREVIEW' AND e.session_id IS NULL FOR UPDATE OF c;
 IF NOT FOUND THEN RAISE EXCEPTION 'PREVIEW_TURN_SCOPE_INVALID'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_admin_preview_turn BEFORE INSERT OR UPDATE OR DELETE ON public.admin_preview_turn FOR EACH ROW EXECUTE FUNCTION public.guard_admin_preview_turn();
