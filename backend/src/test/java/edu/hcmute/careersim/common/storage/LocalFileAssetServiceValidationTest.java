package edu.hcmute.careersim.common.storage;

import static org.assertj.core.api.Assertions.*;

import edu.hcmute.careersim.common.exception.ApiException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;

class LocalFileAssetServiceValidationTest {
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String PPTX =
            "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    @Test
    void acceptsConfirmedFormatsWithMatchingExtensionMimeAndContent() {
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "workbook.xlsx",
                                        XLSX,
                                        archive("[Content_Types].xml", "xl/workbook.xml")))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "slides.pptx",
                                        PPTX,
                                        archive("[Content_Types].xml", "ppt/presentation.xml")))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "code.zip", "application/zip", archive("src/Main.java")))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "tests.py",
                                        "text/x-python",
                                        "def test_ok():\n    assert True\n"
                                                .getBytes(StandardCharsets.UTF_8)))
                .doesNotThrowAnyException();
    }

    @Test
    void preservesExistingSafeFormats() {
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "answer.txt",
                                        "text/plain",
                                        "Reviewed work".getBytes(StandardCharsets.UTF_8)))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "answer.pdf",
                                        "application/pdf",
                                        "%PDF-1.7 fixture".getBytes(StandardCharsets.US_ASCII)))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "image.png",
                                        "image/png",
                                        new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0}))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "image.jpg",
                                        "image/jpeg",
                                        new byte[] {(byte) 255, (byte) 216, (byte) 255, 0}))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                LocalFileAssetService.validateMime(
                                        "notes.md",
                                        "text/markdown",
                                        "# Notes".getBytes(StandardCharsets.UTF_8)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsExtensionMimeAndOfficeStructureMismatches() {
        assertRejected("workbook.zip", XLSX, archive("[Content_Types].xml", "xl/workbook.xml"));
        assertRejected("workbook.xlsx", PPTX, archive("[Content_Types].xml", "xl/workbook.xml"));
        assertRejected("slides.pptx", PPTX, archive("[Content_Types].xml", "xl/workbook.xml"));
        assertRejected("answer.exe", "text/plain", "safe text".getBytes(StandardCharsets.UTF_8));
        assertRejected(
                "answer.txt",
                "application/pdf",
                "%PDF-1.7 fixture".getBytes(StandardCharsets.US_ASCII));
        assertRejected(
                "image.jpg", "image/png", new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0});
        assertRejected(
                "tests.txt", "text/x-python", "print('safe')".getBytes(StandardCharsets.UTF_8));
        assertRejected("tests.py", "text/x-python", new byte[] {(byte) 0xc3, (byte) 0x28});
    }

    @Test
    void rejectsUnsafeOrSuspiciousZipArchives() {
        assertRejected("code.zip", "application/zip", archive("../outside.txt"));
        assertRejected("code.zip", "application/zip", archive("/absolute.txt"));
        assertRejected("code.zip", "application/zip", archive("C:/drive.txt"));
        assertRejected("code.zip", "application/zip", archiveWithRepeatedBytes(1024 * 1024));
    }

    private static void assertRejected(String name, String mime, byte[] bytes) {
        assertThatThrownBy(() -> LocalFileAssetService.validateMime(name, mime, bytes))
                .isInstanceOf(ApiException.class);
    }

    private static byte[] archive(String... names) {
        try {
            var output = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(output)) {
                for (String name : names) {
                    zip.putNextEntry(new ZipEntry(name));
                    zip.write("fixture".getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] archiveWithRepeatedBytes(int size) {
        try {
            var output = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(output)) {
                zip.putNextEntry(new ZipEntry("large.txt"));
                zip.write(new byte[size]);
                zip.closeEntry();
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
