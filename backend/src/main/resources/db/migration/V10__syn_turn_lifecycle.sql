ALTER TABLE syn_sessions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE syn_sessions ADD COLUMN pending_plan JSONB;

CREATE TABLE syn_turns (
 id UUID PRIMARY KEY,
 student_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
 request_id UUID NOT NULL,
 request_hash VARCHAR(64) NOT NULL,
 session_id UUID REFERENCES syn_sessions(id) ON DELETE SET NULL,
 session_version BIGINT NOT NULL,
 state VARCHAR(20) NOT NULL CHECK (state IN ('PENDING','COMPLETED','FAILED','CANCELLED')),
 source VARCHAR(16),
 reason VARCHAR(64),
 model_name VARCHAR(120),
 response JSONB,
 generation_ms INTEGER,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 completed_at TIMESTAMPTZ,
 response_expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),
 UNIQUE(student_id, request_id)
);
CREATE UNIQUE INDEX idx_syn_one_pending_session ON syn_turns(session_id) WHERE state = 'PENDING';
CREATE INDEX idx_syn_turn_student_created ON syn_turns(student_id, created_at);
