package edu.hcmute.careersim.common.storage;

public interface FileAssetService {
    Asset upload(long actor, String name, String mime, byte[] bytes);

    Asset requireReady(long actor, long id, Long enterprise);

    Download download(long actor, long id, String useType, long useId);

    record Asset(
            String id,
            String originalName,
            String mimeType,
            long sizeBytes,
            String checksum,
            String state) {}

    record Download(Asset asset, byte[] bytes) {}
}
