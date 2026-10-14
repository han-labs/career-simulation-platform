package edu.hcmute.careersim.common.storage;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LocalFileAssetService implements FileAssetService {
    private final FileAssetDao dao;
    private final PrivateFileStorage storage;
    private final ActorAccess actors;
    private final FileUploadContext upload;
    private final List<FileUseAuthorizer> policies;
    private final AuditWriter audit;
    private final TransactionTemplate tx;

    public LocalFileAssetService(
            FileAssetDao dao,
            PrivateFileStorage storage,
            ActorAccess actors,
            FileUploadContext upload,
            List<FileUseAuthorizer> policies,
            AuditWriter audit,
            PlatformTransactionManager tm) {
        this.dao = dao;
        this.storage = storage;
        this.actors = actors;
        this.upload = upload;
        this.policies = policies;
        this.audit = audit;
        tx = new TransactionTemplate(tm);
    }

    public Asset upload(long actor, String name, String mime, byte[] bytes) {
        actors.requireId(actor);
        CoreValues.text(name, 255);
        if (name.contains("/")
                || name.contains("\\")
                || name.chars().anyMatch(Character::isISOControl))
            CoreValues.invalid("Invalid file name.");
        if (bytes == null || bytes.length == 0 || bytes.length > 10485760)
            CoreValues.invalid("File must be 1 byte to 10 MiB.");
        validateMime(mime, bytes);
        Long enterprise = upload.enterpriseFor(actor);
        String hash = CoreValues.bytesHash(bytes);
        String key = storage.put(bytes);
        return tx.execute(
                s -> {
                    actors.lock(actor);
                    if (!Objects.equals(enterprise, upload.enterpriseFor(actor)))
                        throw new ApiException(ErrorCode.CONFLICT, "Upload context changed.");
                    long id = dao.stage(actor, enterprise, key, name, mime, bytes.length, hash);
                    dao.ready(id);
                    audit.record(actor, "FILE_READY", "file_asset", id, Map.of("hash", hash));
                    return dao.get(id).asset();
                });
    }

    private void validateMime(String mime, byte[] b) {
        boolean valid =
                switch (mime == null ? "" : mime) {
                    case "text/plain" -> {
                        try {
                            String text =
                                    StandardCharsets.UTF_8
                                            .newDecoder()
                                            .onMalformedInput(CodingErrorAction.REPORT)
                                            .decode(ByteBuffer.wrap(b))
                                            .toString();
                            yield text.indexOf('\0') < 0;
                        } catch (CharacterCodingException e) {
                            yield false;
                        }
                    }
                    case "application/pdf" ->
                            b.length > 5
                                    && new String(b, 0, 5, StandardCharsets.US_ASCII)
                                            .equals("%PDF-");
                    case "image/png" ->
                            b.length > 8
                                    && Arrays.equals(
                                            Arrays.copyOf(b, 8),
                                            new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
                    case "image/jpeg" ->
                            b.length > 3
                                    && b[0] == (byte) 255
                                    && b[1] == (byte) 216
                                    && b[2] == (byte) 255;
                    default -> false;
                };
        if (!valid) CoreValues.invalid("Unsupported or mismatched file type.");
    }

    public Asset requireReady(long actor, long id, Long enterprise) {
        actors.requireId(actor);
        var r =
                dao.get(
                        id,
                        org.springframework.transaction.support.TransactionSynchronizationManager
                                        .isActualTransactionActive()
                                && !org.springframework.transaction.support
                                        .TransactionSynchronizationManager
                                        .isCurrentTransactionReadOnly());
        if (!"READY".equals(r.asset().state())
                || (enterprise == null ? r.owner() != actor : !enterprise.equals(r.enterprise())))
            throw new ApiException(ErrorCode.FORBIDDEN, "Owned READY file is required.");
        return r.asset();
    }

    public Download download(long actor, long id, String useType, long useId) {
        actors.requireId(actor);
        var policy =
                policies.stream()
                        .filter(p -> p.supports(useType))
                        .findFirst()
                        .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
        policy.authorize(actor, id, useType, useId);
        var r = dao.get(id);
        if (!"READY".equals(r.asset().state())) throw new NotFoundException("Asset unavailable.");
        byte[] bytes = storage.read(r.key());
        if (bytes.length != r.asset().sizeBytes()
                || !CoreValues.bytesHash(bytes).equals(r.asset().checksum()))
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "File integrity check failed.");
        return new Download(r.asset(), bytes);
    }
}
