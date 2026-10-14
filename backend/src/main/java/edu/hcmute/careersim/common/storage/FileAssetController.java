package edu.hcmute.careersim.common.storage;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v2/files")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class FileAssetController {
    private final FileAssetService files;
    private final ActorAccess actors;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> upload(Principal p, @RequestPart("file") MultipartFile file)
            throws java.io.IOException {
        return ApiResponse.success(
                files.upload(
                        actors.require(p).id(),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            Principal p,
            @PathVariable String id,
            @RequestParam String useType,
            @RequestParam String useId) {
        var d =
                files.download(
                        actors.require(p).id(), CoreValues.id(id), useType, CoreValues.id(useId));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(
                                        d.asset().originalName(),
                                        java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(d.bytes());
    }
}
