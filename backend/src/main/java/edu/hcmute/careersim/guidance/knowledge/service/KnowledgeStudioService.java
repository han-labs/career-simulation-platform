package edu.hcmute.careersim.guidance.knowledge.service;

import static edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.*;

import java.util.*;

public interface KnowledgeStudioService {
    List<Base> list(long admin);

    Base create(long admin, BaseInput input);

    Base get(long admin, UUID kb);

    Base update(long admin, UUID kb, long revision, BaseInput input);

    Base archive(long admin, UUID kb, long revision);

    Job upload(long admin, UUID kb, Upload input, String name, String mime, byte[] bytes);

    Job job(long admin, UUID kb, UUID job);

    List<Job> jobs(long admin, UUID kb);

    Job retry(long admin, UUID kb, UUID job);

    Job cancel(long admin, UUID kb, UUID job);

    List<AssetVersion> versions(long admin, UUID kb);

    AssetVersion version(long admin, UUID kb, String key, long version);

    AssetVersion publish(
            long admin, UUID kb, String key, long version, long revision, boolean confirmed);

    AssetVersion retire(long admin, UUID kb, String key, long version);

    Passage source(long admin, UUID kb, long chunk);

    List<Passage> search(
            long admin,
            UUID configuration,
            Set<UUID> bases,
            String query,
            int topK,
            boolean embedding);

    void requireAvailable(long admin, UUID configuration, Set<UUID> bases);

    void lockAvailable(long admin, UUID configuration, Set<UUID> bases, List<Passage> evidence);

    boolean evidenceAvailable(long admin, UUID configuration, List<Passage> evidence);
}
