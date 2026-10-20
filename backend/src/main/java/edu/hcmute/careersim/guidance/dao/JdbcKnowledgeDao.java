package edu.hcmute.careersim.guidance.dao;

import edu.hcmute.careersim.guidance.domain.KnowledgeChunk;
import edu.hcmute.careersim.guidance.domain.KnowledgeChunker;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
class JdbcKnowledgeDao implements KnowledgeDao {
    private final JdbcTemplate jdbc;
    private static final String SELECT =
            """
        SELECT c.id,c.version_id,d.title,d.source_key,c.section,c.content,v.language,d.source_url,
        %s AS rank FROM knowledge_chunks c JOIN knowledge_versions v ON v.id=c.version_id
        JOIN knowledge_documents d ON d.id=v.document_id
        WHERE d.status='PUBLISHED' AND d.active_version_id=v.id
        AND NOT EXISTS(SELECT 1 FROM knowledge_document_scope scoped WHERE scoped.document_id=d.id)
        """;
    private final RowMapper<KnowledgeChunk> mapping =
            (rs, i) ->
                    new KnowledgeChunk(
                            rs.getLong("id"),
                            rs.getLong("version_id"),
                            rs.getString("title"),
                            rs.getString("section"),
                            rs.getString("content"),
                            rs.getString("language"),
                            rs.getString("source_url"),
                            rs.getDouble("rank"),
                            rs.getString("source_key"));

    public List<KnowledgeChunk> vector(double[] values, String profile, int limit, double minimum) {
        String vector = vectorLiteral(values);
        return jdbc.query(
                SELECT.formatted("1-(c.embedding <=> CAST(? AS vector))")
                        + " AND v.embedding_profile=? AND 1-(c.embedding <=> CAST(? AS vector))>=? ORDER BY c.embedding <=> CAST(? AS vector), c.id LIMIT ?",
                mapping,
                vector,
                profile,
                vector,
                minimum,
                vector,
                Math.min(20, limit));
    }

    public List<KnowledgeChunk> lexical(String query, int limit) {
        if (query.isBlank()) return List.of();
        return jdbc.query(
                SELECT.formatted("ts_rank_cd(c.search_text,to_tsquery('simple',?))")
                        + " AND c.search_text @@ to_tsquery('simple',?) ORDER BY rank DESC,c.id LIMIT ?",
                mapping,
                query,
                query,
                Math.min(20, limit));
    }

    public Optional<KnowledgeChunk> published(long id) {
        return jdbc
                .query(SELECT.formatted("1.0") + " AND c.id=? FOR SHARE OF d", mapping, id)
                .stream()
                .findFirst();
    }

    @Transactional(timeout = 10)
    public long publish(Document d, String hash, String profile, List<EmbeddedChunk> chunks) {
        jdbc.update(
                "INSERT INTO knowledge_documents(source_key,title,source_url,license,status) VALUES (?,?,?,?,'REVOKED') ON CONFLICT(source_key) DO NOTHING",
                d.sourceKey(),
                d.title(),
                d.sourceUrl(),
                d.license());
        long doc =
                jdbc.queryForObject(
                        "SELECT id FROM knowledge_documents WHERE source_key=? FOR UPDATE",
                        Long.class,
                        d.sourceKey());
        if (Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM knowledge_document_scope WHERE document_id=?)",
                        Boolean.class,
                        doc)))
            throw new IllegalStateException(
                    "Scoped knowledge requires the authenticated KB Studio workflow.");
        List<Long> existing =
                jdbc.query(
                        "SELECT id FROM knowledge_versions WHERE document_id=? AND content_hash=? AND embedding_profile=?",
                        (rs, i) -> rs.getLong(1),
                        doc,
                        hash,
                        profile);
        long version;
        if (existing.isEmpty()) {
            version =
                    jdbc.queryForObject(
                            "INSERT INTO knowledge_versions(document_id,content_hash,language,track,reviewer,embedding_profile) VALUES (?,?,?,?,?,?) RETURNING id",
                            Long.class,
                            doc,
                            hash,
                            d.language(),
                            d.track(),
                            d.reviewer(),
                            profile);
            for (int i = 0; i < chunks.size(); i++) {
                var c = chunks.get(i);
                jdbc.update(
                        "INSERT INTO knowledge_chunks(version_id,ordinal,section,content,search_text,embedding) VALUES (?,?,?,?,to_tsvector('simple',?),CAST(? AS vector))",
                        version,
                        i,
                        c.section(),
                        c.text(),
                        KnowledgeChunker.normalize(d.title() + " " + c.section() + " " + c.text()),
                        vectorLiteral(c.vector()));
            }
        } else version = existing.get(0);
        jdbc.update(
                "UPDATE knowledge_documents SET title=?,source_url=?,license=?,status='PUBLISHED',active_version_id=? WHERE id=?",
                d.title(),
                d.sourceUrl(),
                d.license(),
                version,
                doc);
        return version;
    }

    public void revoke(String key) {
        jdbc.update(
                "UPDATE knowledge_documents d SET status='REVOKED' WHERE source_key=? AND NOT EXISTS(SELECT 1 FROM knowledge_document_scope s WHERE s.document_id=d.id)",
                key);
    }

    static String vectorLiteral(double[] values) {
        if (values.length != 768
                || Arrays.stream(values).anyMatch(v -> !Double.isFinite(v))
                || Arrays.stream(values).allMatch(v -> v == 0))
            throw new IllegalArgumentException("Expected finite, nonzero 768-dimensional vector");
        return Arrays.toString(values);
    }
}
