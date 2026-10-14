package edu.hcmute.careersim.common.storage;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PrivateFileStorage {
    private final Path configured;

    public PrivateFileStorage(@Value("${app.storage.root:./private-assets}") String root) {
        configured = Path.of(root).toAbsolutePath().normalize();
    }

    private Path path(String key) throws IOException {
        if (!key.matches("[a-f0-9-]{36}")) throw new IOException("Invalid storage key");
        Files.createDirectories(configured);
        if (Files.isSymbolicLink(configured)) throw new IOException("Symlink root denied");
        Path root = configured.toRealPath();
        Path p = root.resolve(key).normalize();
        if (!p.getParent().equals(root) || Files.isSymbolicLink(p))
            throw new IOException("Storage escape denied");
        return p;
    }

    public String put(byte[] bytes) {
        String key = UUID.randomUUID().toString();
        try {
            Files.write(
                    path(key),
                    bytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS);
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("Private storage unavailable", e);
        }
    }

    public byte[] read(String key) {
        try {
            try (var input = Files.newInputStream(path(key), LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes = input.readNBytes(10485761);
                if (bytes.length > 10485760) throw new IOException("Private file exceeds bound");
                return bytes;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Private file unavailable", e);
        }
    }
}
