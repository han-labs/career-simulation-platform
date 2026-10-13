package edu.hcmute.careersim.guidance.service;

import edu.hcmute.careersim.guidance.dao.KnowledgeDao;
import edu.hcmute.careersim.guidance.domain.KnowledgeChunker;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KnowledgeIngestionService {
    private final KnowledgeDao dao;
    private final EmbeddingProvider embeddings;

    public long publish(KnowledgeDao.Document document) {
        if (!document.reviewed()
                || document.reviewer() == null
                || document.reviewer().isBlank()
                || document.license() == null
                || document.license().isBlank()
                || document.license().length() > 240
                || document.reviewer().length() > 180
                || document.track() == null
                || document.track().length() > 60
                || document.sourceKey() == null
                || !document.sourceKey().matches("[a-z0-9-]{1,180}")
                || document.title() == null
                || document.title().isBlank()
                || document.title().length() > 240
                || !java.util.Set.of("EN", "VI").contains(document.language()))
            throw new IllegalArgumentException(
                    "Reviewed provenance, title and language are required.");
        if (document.sourceUrl() != null) {
            var uri = java.net.URI.create(document.sourceUrl());
            if (!"https".equals(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || document.sourceUrl().length() > 600)
                throw new IllegalArgumentException(
                        "Source URL must be a valid public HTTPS reference.");
        }
        var chunks =
                KnowledgeChunker.split(document.title(), document.content()).stream()
                        .map(
                                c ->
                                        new KnowledgeDao.EmbeddedChunk(
                                                c.section(),
                                                c.text(),
                                                embeddings.embed(
                                                        document.title() + "\n" + c.text(), false)))
                        .toList();
        try {
            String hash =
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(
                                                    (document.title()
                                                                    + "\n"
                                                                    + document.language()
                                                                    + "\n"
                                                                    + document.track()
                                                                    + "\n"
                                                                    + document.sourceUrl()
                                                                    + "\n"
                                                                    + document.license()
                                                                    + "\n"
                                                                    + document.reviewer()
                                                                    + "\n"
                                                                    + document.content())
                                                            .getBytes(StandardCharsets.UTF_8)));
            return dao.publish(document, hash, embeddings.profile(), chunks);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public void revoke(String sourceKey) {
        dao.revoke(sourceKey);
    }
}
