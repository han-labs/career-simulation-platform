package edu.hcmute.careersim.common.storage;

import edu.hcmute.careersim.common.audit.AuditWriter;
import edu.hcmute.careersim.common.exception.*;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LocalFileAssetService implements FileAssetService {
    private static final int MAX_ARCHIVE_ENTRIES = 1000;
    private static final long MAX_ARCHIVE_EXPANDED_BYTES = 50L * 1024 * 1024;
    private static final int MAX_ARCHIVE_RATIO = 100;
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PPTX =
            "application/vnd.openxmlformats-officedocument.presentationml.presentation";
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
        return upload(actor, name, mime, bytes, false);
    }

    public Asset uploadKnowledgeSource(long admin, String name, String mime, byte[] bytes) {
        ActorAccess.requireRole(actors.requireId(admin), "ADMIN");
        return upload(admin, name, mime, bytes, true);
    }

    private Asset upload(long actor, String name, String mime, byte[] bytes, boolean knowledge) {
        actors.requireId(actor);
        CoreValues.text(name, 255);
        if (name.contains("/")
                || name.contains("\\")
                || name.chars().anyMatch(Character::isISOControl))
            CoreValues.invalid("Invalid file name.");
        if (bytes == null || bytes.length == 0 || bytes.length > 10485760)
            CoreValues.invalid("File must be 1 byte to 10 MiB.");
        boolean office =
                knowledge
                        && Set.of(
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                .contains(mime);
        if (office) {
            if (bytes.length < 4
                    || bytes[0] != 'P'
                    || bytes[1] != 'K'
                    || bytes[2] != 3
                    || bytes[3] != 4) CoreValues.invalid("Invalid Office source signature.");
        } else validateMime(name, mime, bytes);
        String storedMime =
                !knowledge && "application/x-zip-compressed".equals(mime)
                        ? "application/zip"
                        : !knowledge && "text/x-python".equals(mime) ? "text/plain" : mime;
        Long enterprise = upload.enterpriseFor(actor);
        String hash = CoreValues.bytesHash(bytes);
        String key = storage.put(bytes);
        return tx.execute(
                s -> {
                    actors.lock(actor);
                    if (knowledge) ActorAccess.requireRole(actors.requireId(actor), "ADMIN");
                    if (!Objects.equals(enterprise, upload.enterpriseFor(actor)))
                        throw new ApiException(ErrorCode.CONFLICT, "Upload context changed.");
                    long id =
                            dao.stage(actor, enterprise, key, name, storedMime, bytes.length, hash);
                    dao.ready(id);
                    audit.record(actor, "FILE_READY", "file_asset", id, Map.of("hash", hash));
                    return dao.get(id).asset();
                });
    }

    static void validateMime(String name, String mime, byte[] b) {
        String extension = extension(name);
        boolean valid =
                switch (mime == null ? "" : mime) {
                    case "text/plain" -> Set.of("txt", "py").contains(extension) && utf8(b);
                    case "text/markdown" -> extension.equals("md") && utf8(b);
                    case "text/x-python" -> extension.equals("py") && utf8(b);
                    case "application/pdf" ->
                            extension.equals("pdf")
                                    && b.length > 5
                                    && new String(b, 0, 5, StandardCharsets.US_ASCII)
                                            .equals("%PDF-");
                    case "image/png" ->
                            extension.equals("png")
                                    && b.length > 8
                                    && Arrays.equals(
                                            Arrays.copyOf(b, 8),
                                            new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
                    case "image/jpeg" ->
                            Set.of("jpg", "jpeg").contains(extension)
                                    && b.length > 3
                                    && b[0] == (byte) 255
                                    && b[1] == (byte) 216
                                    && b[2] == (byte) 255;
                    case XLSX -> extension.equals("xlsx") && officeArchive(b, "xl/workbook.xml");
                    case PPTX ->
                            extension.equals("pptx") && officeArchive(b, "ppt/presentation.xml");
                    case "application/zip", "application/x-zip-compressed" ->
                            extension.equals("zip") && safeArchive(b, (String) null);
                    default -> false;
                };
        if (!valid) CoreValues.invalid("Unsupported or mismatched file type.");
    }

    private static boolean utf8(byte[] bytes) {
        try {
            String text =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes))
                            .toString();
            return text.indexOf('\0') < 0;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static String extension(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        return dot < 1 || dot == name.length() - 1
                ? ""
                : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean officeArchive(byte[] bytes, String requiredPart) {
        return safeArchive(bytes, Set.of("[Content_Types].xml", requiredPart));
    }

    private static boolean safeArchive(byte[] bytes, String requiredPart) {
        return safeArchive(bytes, requiredPart == null ? Set.of() : Set.of(requiredPart));
    }

    private static boolean safeArchive(byte[] bytes, Set<String> requiredParts) {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') return false;
        Set<String> names = new HashSet<>();
        long expanded = 0;
        int entries = 0;
        byte[] buffer = new byte[8192];
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (++entries > MAX_ARCHIVE_ENTRIES || !safeEntry(entry.getName())) return false;
                if (!names.add(entry.getName())) return false;
                if (entry.getSize() > MAX_ARCHIVE_EXPANDED_BYTES) return false;
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    expanded += read;
                    if (expanded > MAX_ARCHIVE_EXPANDED_BYTES
                            || expanded > (long) bytes.length * MAX_ARCHIVE_RATIO) return false;
                }
                zip.closeEntry();
            }
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
        return entries > 0 && names.containsAll(requiredParts);
    }

    private static boolean safeEntry(String name) {
        if (name == null || name.isBlank() || name.indexOf('\0') >= 0) return false;
        String normalized = name.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) return false;
        return Arrays.stream(normalized.split("/", -1)).noneMatch(".."::equals);
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
