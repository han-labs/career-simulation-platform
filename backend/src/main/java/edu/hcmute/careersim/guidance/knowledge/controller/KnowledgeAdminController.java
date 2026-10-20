package edu.hcmute.careersim.guidance.knowledge.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.guidance.agent.service.ExecutionService;
import edu.hcmute.careersim.guidance.knowledge.dto.KnowledgeContracts.*;
import edu.hcmute.careersim.guidance.knowledge.service.KnowledgeStudioService;
import edu.hcmute.careersim.identity.access.ActorAccess;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v2/admin/knowledge-bases")
@ConditionalOnProperty(name = "app.agent-platform-enabled", havingValue = "true")
public class KnowledgeAdminController {
    private final KnowledgeStudioService studio;
    private final ExecutionService executions;
    private final ActorAccess actors;

    @GetMapping
    public ApiResponse<?> list(Principal p) {
        return ApiResponse.success(studio.list(actors.admin(p).id()));
    }

    @PostMapping
    public ApiResponse<?> create(Principal p, @RequestBody BaseInput r) {
        return ApiResponse.success(studio.create(actors.admin(p).id(), r));
    }

    @GetMapping("/{kb}")
    public ApiResponse<?> get(Principal p, @PathVariable UUID kb) {
        return ApiResponse.success(studio.get(actors.admin(p).id(), kb));
    }

    @PatchMapping("/{kb}")
    public ApiResponse<?> update(
            Principal p,
            @PathVariable UUID kb,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @RequestBody BaseInput r) {
        return ApiResponse.success(
                studio.update(actors.admin(p).id(), kb, CoreValues.revision(revision), r));
    }

    @DeleteMapping("/{kb}")
    public ApiResponse<?> archive(
            Principal p,
            @PathVariable UUID kb,
            @RequestHeader(value = "If-Match", required = false) String revision) {
        return ApiResponse.success(
                studio.archive(actors.admin(p).id(), kb, CoreValues.revision(revision)));
    }

    @GetMapping("/{kb}/assets")
    public ApiResponse<?> assets(Principal p, @PathVariable UUID kb) {
        return ApiResponse.success(studio.versions(actors.admin(p).id(), kb));
    }

    @GetMapping("/{kb}/ingestions")
    public ApiResponse<?> jobs(Principal p, @PathVariable UUID kb) {
        return ApiResponse.success(studio.jobs(actors.admin(p).id(), kb));
    }

    @PostMapping(value = "/{kb}/assets", consumes = "multipart/form-data")
    public ApiResponse<?> upload(
            Principal p,
            @PathVariable UUID kb,
            @RequestPart("metadata") Upload metadata,
            @RequestPart("file") MultipartFile file)
            throws java.io.IOException {
        return ApiResponse.success(
                studio.upload(
                        actors.admin(p).id(),
                        kb,
                        metadata,
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes()));
    }

    @GetMapping("/{kb}/ingestions/{job}")
    public ApiResponse<?> job(Principal p, @PathVariable UUID kb, @PathVariable UUID job) {
        return ApiResponse.success(studio.job(actors.admin(p).id(), kb, job));
    }

    @PostMapping("/{kb}/ingestions/{job}/retry")
    public ApiResponse<?> retry(Principal p, @PathVariable UUID kb, @PathVariable UUID job) {
        return ApiResponse.success(studio.retry(actors.admin(p).id(), kb, job));
    }

    @PostMapping("/{kb}/ingestions/{job}/cancel")
    public ApiResponse<?> cancel(Principal p, @PathVariable UUID kb, @PathVariable UUID job) {
        return ApiResponse.success(studio.cancel(actors.admin(p).id(), kb, job));
    }

    @GetMapping("/{kb}/assets/{key}/versions/{version}")
    public ApiResponse<?> version(
            Principal p,
            @PathVariable UUID kb,
            @PathVariable String key,
            @PathVariable String version) {
        return ApiResponse.success(
                studio.version(actors.admin(p).id(), kb, key, CoreValues.id(version)));
    }

    @PostMapping("/{kb}/assets/{key}/versions/{version}/publish")
    public ApiResponse<?> publish(
            Principal p,
            @PathVariable UUID kb,
            @PathVariable String key,
            @PathVariable String version,
            @RequestHeader(value = "If-Match", required = false) String revision,
            @RequestBody Confirmation r) {
        return ApiResponse.success(
                studio.publish(
                        actors.admin(p).id(),
                        kb,
                        key,
                        CoreValues.id(version),
                        CoreValues.revision(revision),
                        r.confirmed()));
    }

    @PostMapping("/{kb}/assets/{key}/versions/{version}/retire")
    public ApiResponse<?> retire(
            Principal p,
            @PathVariable UUID kb,
            @PathVariable String key,
            @PathVariable String version) {
        return ApiResponse.success(
                studio.retire(actors.admin(p).id(), kb, key, CoreValues.id(version)));
    }

    @PostMapping("/{kb}/tests")
    public ResponseEntity<?> test(Principal p, @PathVariable UUID kb, @RequestBody Test r) {
        return ResponseEntity.accepted()
                .body(ApiResponse.success(executions.test(actors.admin(p).id(), kb, r)));
    }

    @GetMapping("/{kb}/sources/{chunk}")
    public ApiResponse<?> source(Principal p, @PathVariable UUID kb, @PathVariable String chunk) {
        return ApiResponse.success(studio.source(actors.admin(p).id(), kb, CoreValues.id(chunk)));
    }
}
