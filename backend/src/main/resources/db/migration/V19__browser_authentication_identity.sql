-- Additive P1 identity changes. No historical identity, consent or ownership is inferred.
ALTER TABLE public.app_users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE public.app_users ADD COLUMN auth_version bigint NOT NULL DEFAULT 0 CHECK (auth_version >= 0);
ALTER TABLE public.enterprise ADD COLUMN description text;

CREATE TABLE public.external_identity (
 id uuid PRIMARY KEY,
 user_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 issuer varchar(500) NOT NULL,
 subject varchar(255) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT uq_external_identity_subject UNIQUE (issuer, subject)
);
CREATE INDEX ix_external_identity_user ON public.external_identity(user_id);

-- Only a credential identifier is stored, never a JWT, provider token or secret.
CREATE TABLE public.auth_token_revocation (
 token_id uuid PRIMARY KEY,
 user_id bigint NOT NULL REFERENCES public.app_users(id) ON DELETE RESTRICT,
 expires_at timestamptz NOT NULL,
 revoked_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_auth_token_revocation_expiry ON public.auth_token_revocation(expires_at);

CREATE FUNCTION public.invalidate_account_credentials() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.status IS DISTINCT FROM OLD.status OR NEW.password_hash IS DISTINCT FROM OLD.password_hash
    OR NEW.role IS DISTINCT FROM OLD.role THEN
   NEW.auth_version := OLD.auth_version + 1;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER tr_app_users_auth_version BEFORE UPDATE ON public.app_users
 FOR EACH ROW EXECUTE FUNCTION public.invalidate_account_credentials();
