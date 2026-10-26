-- Null preserves the exact v1 renderer/hash and all existing sealed proofs.
ALTER TABLE public.agent_configuration ADD COLUMN experience_config jsonb
    CHECK (experience_config IS NULL OR
        (jsonb_typeof(experience_config) = 'object' AND experience_config ? 'schemaVersion'
         AND experience_config->>'schemaVersion' = '2'));
