CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE knowledge_documents (
 id BIGSERIAL PRIMARY KEY,
 source_key VARCHAR(180) NOT NULL UNIQUE,
 title VARCHAR(240) NOT NULL,
 source_url VARCHAR(600),
 license VARCHAR(240) NOT NULL,
 status VARCHAR(16) NOT NULL CHECK (status IN ('PUBLISHED','REVOKED')),
 active_version_id BIGINT
);
CREATE TABLE knowledge_versions (
 id BIGSERIAL PRIMARY KEY,
 document_id BIGINT NOT NULL REFERENCES knowledge_documents(id),
 content_hash VARCHAR(64) NOT NULL,
 language VARCHAR(8) NOT NULL,
 track VARCHAR(60) NOT NULL,
 reviewer VARCHAR(180) NOT NULL,
 reviewed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 embedding_profile VARCHAR(180) NOT NULL,
 UNIQUE(document_id,content_hash,embedding_profile)
);
ALTER TABLE knowledge_documents ADD CONSTRAINT fk_active_knowledge_version
 FOREIGN KEY(active_version_id) REFERENCES knowledge_versions(id);
CREATE TABLE knowledge_chunks (
 id BIGSERIAL PRIMARY KEY,
 version_id BIGINT NOT NULL REFERENCES knowledge_versions(id),
 ordinal INTEGER NOT NULL,
 section VARCHAR(240) NOT NULL,
 content TEXT NOT NULL,
 search_text TSVECTOR NOT NULL,
 embedding VECTOR(768) NOT NULL,
 UNIQUE(version_id,ordinal)
);
CREATE INDEX idx_knowledge_lexical ON knowledge_chunks USING GIN(search_text);
CREATE INDEX idx_knowledge_version ON knowledge_chunks(version_id);
