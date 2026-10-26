package edu.hcmute.careersim.guidance.knowledge.dao;

import static edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.*;

import com.fasterxml.jackson.databind.JsonNode;
import edu.hcmute.careersim.common.exception.NotFoundException;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.domain.KnowledgeChunker;
import edu.hcmute.careersim.guidance.knowledge.infrastructure.KnowledgeParser;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KnowledgeStudioDao {
    private final JdbcTemplate jdbc;
    private final RowMapper<Base> baseMap =
            (r, n) ->
                    new Base(
                            r.getString("id"),
                            r.getString("name"),
                            r.getString("scope_type"),
                            r.getString("visibility"),
                            r.getString("status"),
                            r.getLong("publication_revision"),
                            r.getLong("row_version"),
                            r.getTimestamp("created_at").toInstant());
    private final RowMapper<Job> jobMap =
            (r, n) ->
                    new Job(
                            r.getString("id"),
                            r.getString("knowledge_base_id"),
                            r.getString("asset_key"),
                            r.getString("uploader_id"),
                            r.getString("source_file_id"),
                            r.getString("state"),
                            r.getString("produced_version_id"),
                            r.getString("error_code"),
                            r.getString("parser_profile"),
                            r.getString("embedding_profile"),
                            CoreValues.json(r.getString("metadata_snapshot")),
                            CoreValues.json(r.getString("attempt_history")),
                            r.getLong("row_version"),
                            r.getTimestamp("created_at").toInstant(),
                            r.getTimestamp("started_at") == null
                                    ? null
                                    : r.getTimestamp("started_at").toInstant(),
                            r.getTimestamp("finished_at") == null
                                    ? null
                                    : r.getTimestamp("finished_at").toInstant());

    public List<BaseSummary> list() {
        return jdbc.query(
                """
            WITH bases AS (SELECT * FROM public.knowledge_base WHERE scope_type='PLATFORM' ORDER BY created_at,id LIMIT 200),
            versions AS (SELECT s.knowledge_base_id,s.asset_key,count(*) total,bool_or(m.publication_status<>'RETIRED') live
                FROM public.knowledge_document_scope s JOIN bases b ON b.id=s.knowledge_base_id JOIN public.knowledge_asset_metadata m ON m.document_id=s.document_id GROUP BY 1,2),
            jobs AS (SELECT j.knowledge_base_id,j.asset_key,bool_or(j.state NOT IN ('CANCELLED','READY','PUBLISHED')) pending,bool_or(j.state<>'CANCELLED') live
                FROM public.knowledge_ingestion_job j JOIN bases b ON b.id=j.knowledge_base_id GROUP BY 1,2),
            docs AS (SELECT coalesce(v.knowledge_base_id,j.knowledge_base_id) kb,count(*) n FROM versions v FULL JOIN jobs j USING(knowledge_base_id,asset_key)
                WHERE coalesce(v.live,false) OR (v.total IS NOT NULL AND coalesce(j.pending,false)) OR (v.total IS NULL AND coalesce(j.live,false)) GROUP BY 1),
            chunks AS (SELECT s.knowledge_base_id kb,count(*) n FROM public.knowledge_document_scope s JOIN bases b ON b.id=s.knowledge_base_id
                JOIN public.knowledge_documents d ON d.id=s.document_id AND d.status='PUBLISHED'
                JOIN public.knowledge_asset_metadata m ON m.version_id=d.active_version_id AND m.publication_status='PUBLISHED'
                JOIN public.knowledge_chunks c ON c.version_id=d.active_version_id AND c.embedding IS NOT NULL GROUP BY 1)
            SELECT b.*,coalesce(d.n,0) document_count,coalesce(c.n,0) published_chunk_count FROM bases b LEFT JOIN docs d ON d.kb=b.id LEFT JOIN chunks c ON c.kb=b.id ORDER BY b.created_at,b.id
            """,
                (r, n) ->
                        new BaseSummary(
                                r.getString("id"),
                                r.getString("name"),
                                r.getString("scope_type"),
                                r.getString("visibility"),
                                r.getString("status"),
                                r.getLong("publication_revision"),
                                r.getLong("row_version"),
                                r.getTimestamp("created_at").toInstant(),
                                r.getLong("document_count"),
                                r.getLong("published_chunk_count")));
    }

    public Base base(UUID id, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM public.knowledge_base WHERE id=? AND scope_type='PLATFORM'"
                                + (lock ? " FOR UPDATE" : ""),
                        baseMap,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Knowledge base not found."));
    }

    public Base create(long actor, String name) {
        var id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.knowledge_base(id,name,scope_type,created_by_admin_id) VALUES (?,?,'PLATFORM',?)",
                id,
                name,
                actor);
        return base(id, false);
    }

    public void update(UUID id, String name, String status) {
        jdbc.update(
                "UPDATE public.knowledge_base SET name=?,status=?,updated_at=CURRENT_TIMESTAMP,row_version=row_version+1,publication_revision=publication_revision+1 WHERE id=?",
                name,
                status,
                id);
    }

    public Optional<Job> request(long actor, UUID request) {
        return jdbc
                .query(
                        "SELECT * FROM public.knowledge_ingestion_job WHERE uploader_id=? AND request_id=?",
                        jobMap,
                        actor,
                        request)
                .stream()
                .findFirst();
    }

    public String inputHash(UUID id) {
        return jdbc.queryForObject(
                "SELECT input_hash FROM public.knowledge_ingestion_job WHERE id=?",
                String.class,
                id);
    }

    public Job job(UUID kb, UUID id, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM public.knowledge_ingestion_job WHERE id=? AND knowledge_base_id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        jobMap,
                        id,
                        kb)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Ingestion not found."));
    }

    public List<Job> jobs(UUID kb) {
        return jdbc.query(
                "SELECT j.id,j.knowledge_base_id,j.asset_key,j.uploader_id,j.source_file_id,j.state,j.produced_version_id,j.error_code,j.parser_profile,j.embedding_profile,j.metadata_snapshot,COALESCE((SELECT jsonb_agg(a - 'extractionSnapshot' ORDER BY ord) FROM jsonb_array_elements(j.attempt_history) WITH ORDINALITY AS items(a,ord)),'[]'::jsonb) AS attempt_history,j.row_version,j.created_at,j.started_at,j.finished_at FROM public.knowledge_ingestion_job j WHERE knowledge_base_id=? ORDER BY created_at DESC,id LIMIT 100",
                jobMap,
                kb);
    }

    public Job createJob(
            UUID kb, long actor, long file, Upload r, String hash, Map<String, Object> metadata) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO public.knowledge_ingestion_job(id,knowledge_base_id,asset_key,uploader_id,source_file_id,request_id,input_hash,metadata_snapshot,parser_profile) VALUES (?,?,?,?,?,?,?,CAST(? AS jsonb),?)",
                id,
                kb,
                r.assetKey(),
                actor,
                file,
                r.requestId(),
                hash,
                CoreValues.write(metadata),
                KnowledgeParser.PROFILE);
        return job(kb, id, false);
    }

    public void claim(UUID id) {
        jdbc.update(
                "UPDATE public.knowledge_ingestion_job SET state='PARSING',error_code=NULL,started_at=CURRENT_TIMESTAMP,finished_at=NULL,row_version=row_version+1 WHERE id=?",
                id);
    }

    public boolean embedding(UUID id, long generation, String profile) {
        return jdbc.update(
                        "UPDATE public.knowledge_ingestion_job SET state='EMBEDDING',embedding_profile=? WHERE id=? AND row_version=? AND state='PARSING'",
                        profile,
                        id,
                        generation)
                == 1;
    }

    public boolean finish(
            UUID id,
            long generation,
            String state,
            Long version,
            String error,
            Map<String, Object> metrics) {
        return jdbc.update(
                        "UPDATE public.knowledge_ingestion_job SET state=?,produced_version_id=?,error_code=?,finished_at=CURRENT_TIMESTAMP,attempt_history=CAST(? AS jsonb),row_version=row_version+1 WHERE id=? AND row_version=? AND state IN ('PARSING','EMBEDDING')",
                        state,
                        version,
                        error,
                        CoreValues.write(mergedAttempt(id, generation, CoreValues.tree(metrics))),
                        id,
                        generation)
                == 1;
    }

    public void cancel(UUID id) {
        jdbc.update(
                "UPDATE public.knowledge_ingestion_job SET state='CANCELLED',error_code='CANCELLED',finished_at=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE id=? AND state IN ('QUEUED','PARSING','EMBEDDING','FAILED')",
                id);
    }

    /** Lock and merge exactly one coherent entry per generation; never replace saved extraction. */
    private JsonNode mergedAttempt(UUID id, long generation, JsonNode patch) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive())
            throw new IllegalStateException("Attempt merge requires a caller transaction.");
        var history =
                (com.fasterxml.jackson.databind.node.ArrayNode)
                        CoreValues.json(
                                jdbc.queryForObject(
                                        "SELECT attempt_history::text FROM public.knowledge_ingestion_job WHERE id=? FOR UPDATE",
                                        String.class,
                                        id));
        com.fasterxml.jackson.databind.node.ObjectNode entry = null;
        for (var attempt : history)
            if (attempt.path("generation").asLong(-1) == generation) {
                entry = (com.fasterxml.jackson.databind.node.ObjectNode) attempt;
                break;
            }
        if (entry == null) {
            entry = history.addObject();
            entry.put("generation", generation);
        }
        var incoming = (com.fasterxml.jackson.databind.node.ObjectNode) patch.deepCopy();
        if (entry.has("extractionSnapshot")) {
            if (incoming.has("extractionSnapshot")
                    && !entry.get("extractionSnapshot").equals(incoming.get("extractionSnapshot")))
                throw new IllegalStateException("Attempt extraction is immutable.");
            incoming.remove("extractionSnapshot");
        }
        entry.setAll(incoming);
        return history;
    }

    public boolean snapshot(UUID kb, UUID id, long generation, JsonNode extraction) {
        var current = job(kb, id, true);
        if (current.revision() != generation || !"PARSING".equals(current.state())) return false;
        var history =
                mergedAttempt(
                        id,
                        generation,
                        CoreValues.tree(
                                Map.of("extractionSnapshot", extraction, "stage", "EXTRACTED")));
        return jdbc.update(
                        "UPDATE public.knowledge_ingestion_job SET attempt_history=CAST(? AS jsonb) WHERE id=? AND row_version=? AND state='PARSING'",
                        CoreValues.write(history),
                        id,
                        generation)
                == 1;
    }

    public void recordAttempt(UUID id, long generation, Map<String, Object> metrics) {
        var history = mergedAttempt(id, generation, CoreValues.tree(metrics));
        jdbc.update(
                "UPDATE public.knowledge_ingestion_job SET attempt_history=CAST(? AS jsonb) WHERE id=?",
                CoreValues.write(history),
                id);
    }

    public boolean linkedSource(long file) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM public.knowledge_ingestion_job j JOIN public.knowledge_base b ON b.id=j.knowledge_base_id WHERE j.source_file_id=? AND b.scope_type='PLATFORM')",
                        Boolean.class,
                        file));
    }

    public Set<UUID> grants(UUID config) {
        return new HashSet<>(
                jdbc.queryForList(
                        "SELECT s.knowledge_base_id FROM public.agent_config_kb_scope s JOIN public.agent_configuration c ON c.id=s.configuration_id JOIN public.agent a ON a.id=c.agent_id WHERE c.id=? AND c.sealed AND a.status<>'ARCHIVED'",
                        UUID.class,
                        config));
    }

    public long stage(
            Job job,
            String hash,
            String profile,
            List<KnowledgeParser.Chunk> chunks,
            List<double[]> vectors,
            JsonNode extractionSnapshot) {
        var m = (com.fasterxml.jackson.databind.node.ObjectNode) job.metadata().deepCopy();
        m.set("extractionSnapshot", extractionSnapshot);
        UUID kb = UUID.fromString(job.knowledgeBaseId());
        String key = "kb-" + kb + "-" + job.assetKey();
        jdbc.update(
                "INSERT INTO public.knowledge_documents(source_key,title,source_url,license,status) VALUES (?,?,?,?,'REVOKED') ON CONFLICT(source_key) DO NOTHING",
                key,
                m.path("title").asText(),
                m.path("sourceUri").isTextual() ? m.path("sourceUri").asText() : null,
                m.path("license").asText());
        long doc =
                jdbc.queryForObject(
                        "SELECT id FROM public.knowledge_documents WHERE source_key=? FOR UPDATE",
                        Long.class,
                        key);
        jdbc.update(
                "INSERT INTO public.knowledge_document_scope(document_id,knowledge_base_id,asset_key) SELECT ?,?,? WHERE NOT EXISTS(SELECT 1 FROM public.knowledge_document_scope WHERE document_id=?)",
                doc,
                kb,
                job.assetKey(),
                doc);
        var previous =
                jdbc.queryForList(
                        "SELECT id FROM public.knowledge_versions WHERE document_id=? AND content_hash=? AND embedding_profile=?",
                        Long.class,
                        doc,
                        hash,
                        profile);
        if (!previous.isEmpty()) return previous.get(0);
        long version =
                jdbc.queryForObject(
                        "INSERT INTO public.knowledge_versions(document_id,content_hash,language,track,reviewer,embedding_profile) VALUES (?,?,?,?,?,?) RETURNING id",
                        Long.class,
                        doc,
                        hash,
                        m.path("language").asText(),
                        m.path("track").asText(),
                        m.path("reviewer").asText(),
                        profile);
        for (int i = 0; i < chunks.size(); i++) {
            var c = chunks.get(i);
            jdbc.update(
                    "INSERT INTO public.knowledge_chunks(version_id,ordinal,section,content,search_text,embedding,source_locator) VALUES (?,?,?,?,to_tsvector('simple',?),CAST(? AS vector),CAST(? AS jsonb))",
                    version,
                    i,
                    c.section(),
                    c.text(),
                    KnowledgeChunker.normalize(
                            m.path("title").asText() + " " + c.section() + " " + c.text()),
                    vector(vectors.get(i)),
                    CoreValues.write(c.locator()));
        }
        int number =
                jdbc.queryForObject(
                        "SELECT COALESCE(max(version_number),0)+1 FROM public.knowledge_asset_metadata WHERE document_id=?",
                        Integer.class,
                        doc);
        jdbc.update(
                "INSERT INTO public.knowledge_asset_metadata(version_id,document_id,version_number,source_file_id,source_uri,mime_type,uploaded_by_admin_id,locator_metadata) VALUES (?,?,?,?,?,?,?,CAST(? AS jsonb))",
                version,
                doc,
                number,
                Long.parseLong(job.sourceFileId()),
                m.path("sourceUri").isTextual() ? m.path("sourceUri").asText() : null,
                m.path("mimeType").asText(),
                Long.parseLong(job.uploaderId()),
                CoreValues.write(m));
        return version;
    }

    public Optional<Long> existing(UUID kb, String key, String hash, String profile) {
        return jdbc
                .query(
                        "SELECT v.id FROM public.knowledge_versions v JOIN public.knowledge_document_scope s ON s.document_id=v.document_id JOIN public.knowledge_asset_metadata m ON m.version_id=v.id WHERE s.knowledge_base_id=? AND s.asset_key=? AND v.content_hash=? AND v.embedding_profile=?",
                        (r, n) -> r.getLong(1),
                        kb,
                        key,
                        hash,
                        profile)
                .stream()
                .findFirst();
    }

    private static final String VERSIONS =
            "SELECT m.*,v.content_hash,v.embedding_profile,v.reviewer,v.language,s.knowledge_base_id,s.asset_key FROM public.knowledge_asset_metadata m JOIN public.knowledge_versions v ON v.id=m.version_id JOIN public.knowledge_document_scope s ON s.document_id=m.document_id WHERE s.knowledge_base_id=?";
    private final RowMapper<AssetVersion> versionMap =
            (r, n) -> {
                var meta = CoreValues.json(r.getString("locator_metadata"));
                return new AssetVersion(
                        r.getString("version_id"),
                        r.getString("document_id"),
                        r.getString("knowledge_base_id"),
                        r.getString("asset_key"),
                        r.getInt("version_number"),
                        meta.path("title").asText(),
                        r.getString("publication_status"),
                        r.getString("content_hash"),
                        r.getString("embedding_profile"),
                        r.getString("mime_type"),
                        r.getString("source_file_id"),
                        r.getString("source_uri"),
                        meta.path("license").asText(),
                        r.getString("reviewer"),
                        r.getString("language"),
                        meta,
                        r.getTimestamp("published_at") == null
                                ? null
                                : r.getTimestamp("published_at").toInstant(),
                        List.of());
            };

    public List<AssetVersion> versions(UUID kb) {
        return jdbc.query(
                VERSIONS.replace(
                                "m.*",
                                "m.version_id,m.document_id,m.version_number,m.mime_type,m.source_file_id,m.source_uri,m.publication_status,m.published_at,(m.locator_metadata - 'extractionSnapshot') AS locator_metadata")
                        + " ORDER BY s.asset_key,m.version_number DESC LIMIT 500",
                versionMap,
                kb);
    }

    public AssetVersion version(UUID kb, String key, long id, boolean lock) {
        return jdbc
                .query(
                        VERSIONS
                                + " AND s.asset_key=? AND m.version_id=?"
                                + (lock ? " FOR UPDATE OF m,v" : ""),
                        versionMap,
                        kb,
                        key,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Knowledge version not found."));
    }

    private static final String PASSAGES =
            "SELECT c.*,v.embedding_profile,s.knowledge_base_id,s.asset_key,m.locator_metadata,m.source_uri FROM public.knowledge_chunks c JOIN public.knowledge_versions v ON v.id=c.version_id JOIN public.knowledge_asset_metadata m ON m.version_id=v.id JOIN public.knowledge_document_scope s ON s.document_id=m.document_id";

    private Passage passage(java.sql.ResultSet r, double score, String method)
            throws java.sql.SQLException {
        return new Passage(
                "K" + r.getLong("id"),
                r.getString("id"),
                r.getString("version_id"),
                r.getString("knowledge_base_id"),
                r.getString("asset_key"),
                CoreValues.json(r.getString("locator_metadata")).path("title").asText(),
                r.getString("content"),
                r.getString("section"),
                CoreValues.json(r.getString("source_locator")),
                r.getString("source_uri"),
                r.getString("embedding_profile"),
                score,
                method);
    }

    public List<Passage> chunks(long version) {
        return jdbc.query(
                PASSAGES + " WHERE c.version_id=? ORDER BY c.ordinal",
                (r, n) -> passage(r, 0, "INSPECTION"),
                version);
    }

    public record Candidates(List<Passage> lexical, List<Passage> vector) {}

    public Candidates candidates(UUID kb, String tsquery, double[] vector, String profile) {
        String eligible =
                " JOIN public.knowledge_documents d ON d.id=v.document_id JOIN public.knowledge_base b ON b.id=s.knowledge_base_id WHERE s.knowledge_base_id=? AND b.scope_type='PLATFORM' AND b.status='ACTIVE' AND d.status='PUBLISHED' AND d.active_version_id=v.id AND m.publication_status='PUBLISHED'";
        List<Passage> lexical =
                tsquery.isBlank()
                        ? List.of()
                        : jdbc.query(
                                PASSAGES
                                        + eligible
                                        + " AND c.search_text @@ to_tsquery('simple',?) ORDER BY ts_rank_cd(c.search_text,to_tsquery('simple',?)) DESC,c.id LIMIT 20",
                                (r, n) -> passage(r, 0, "LEXICAL"),
                                kb,
                                tsquery,
                                tsquery);
        List<Passage> vectors =
                vector == null
                        ? List.of()
                        : jdbc.query(
                                PASSAGES
                                        + eligible
                                        + " AND v.embedding_profile=? AND 1-(c.embedding <=> CAST(? AS vector))>=0.5 ORDER BY c.embedding <=> CAST(? AS vector),c.id LIMIT 20",
                                (r, n) -> passage(r, 0, "VECTOR"),
                                kb,
                                profile,
                                vector(vector),
                                vector(vector));
        return new Candidates(lexical, vectors);
    }

    public Optional<Passage> source(UUID kb, long chunk) {
        return jdbc
                .query(
                        PASSAGES
                                + " JOIN public.knowledge_documents d ON d.id=v.document_id JOIN public.knowledge_base b ON b.id=s.knowledge_base_id WHERE c.id=? AND s.knowledge_base_id=? AND b.scope_type='PLATFORM' AND b.status='ACTIVE' AND d.status='PUBLISHED' AND d.active_version_id=v.id AND m.publication_status='PUBLISHED'",
                        (r, n) -> passage(r, 0, "SOURCE"),
                        chunk,
                        kb)
                .stream()
                .findFirst();
    }

    public Optional<Passage> lockSource(UUID kb, long chunk) {
        return jdbc
                .query(
                        PASSAGES
                                + " JOIN public.knowledge_documents d ON d.id=v.document_id JOIN public.knowledge_base b ON b.id=s.knowledge_base_id WHERE c.id=? AND s.knowledge_base_id=? AND b.scope_type='PLATFORM' AND b.status='ACTIVE' AND d.status='PUBLISHED' AND d.active_version_id=v.id AND m.publication_status='PUBLISHED' FOR SHARE OF d,m",
                        (r, n) -> passage(r, 0, "SOURCE"),
                        chunk,
                        kb)
                .stream()
                .findFirst();
    }

    public void publish(AssetVersion v) {
        long doc = Long.parseLong(v.documentId()), id = Long.parseLong(v.id());
        jdbc.queryForObject(
                "SELECT id FROM public.knowledge_documents WHERE id=? FOR UPDATE", Long.class, doc);
        jdbc.update(
                "UPDATE public.knowledge_asset_metadata SET publication_status='RETIRED' WHERE document_id=? AND publication_status='PUBLISHED' AND version_id<>?",
                doc,
                id);
        jdbc.update(
                "UPDATE public.knowledge_asset_metadata SET publication_status='PUBLISHED',published_at=COALESCE(published_at,CURRENT_TIMESTAMP) WHERE version_id=?",
                id);
        jdbc.update(
                "UPDATE public.knowledge_documents SET status='PUBLISHED',active_version_id=? WHERE id=?",
                id,
                doc);
        bump(UUID.fromString(v.knowledgeBaseId()));
        jdbc.update(
                "UPDATE public.knowledge_ingestion_job SET state='PUBLISHED' WHERE produced_version_id=? AND state='READY'",
                id);
    }

    public void retire(AssetVersion v) {
        jdbc.update(
                "UPDATE public.knowledge_asset_metadata SET publication_status='RETIRED' WHERE version_id=?",
                Long.parseLong(v.id()));
        bump(UUID.fromString(v.knowledgeBaseId()));
    }

    private void bump(UUID kb) {
        jdbc.update(
                "UPDATE public.knowledge_base SET publication_revision=publication_revision+1,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                kb);
    }

    public static String vector(double[] v) {
        if (v == null
                || v.length != 768
                || Arrays.stream(v).anyMatch(x -> !Double.isFinite(x))
                || Arrays.stream(v).allMatch(x -> x == 0))
            throw new IllegalArgumentException("Invalid embedding vector.");
        return Arrays.toString(v);
    }
}
