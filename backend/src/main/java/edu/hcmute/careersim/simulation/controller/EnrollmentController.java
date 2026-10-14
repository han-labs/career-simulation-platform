package edu.hcmute.careersim.simulation.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import edu.hcmute.careersim.common.value.CoreValues;
import edu.hcmute.careersim.identity.access.ActorAccess;
import edu.hcmute.careersim.simulation.dto.ScenarioContracts.*;
import edu.hcmute.careersim.simulation.service.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.core-v2-enabled", havingValue = "true")
public class EnrollmentController {
    private final EnrollmentService enrollments;
    private final TaskSubmissionService submissions;
    private final ModelAnswerAccess answers;
    private final CompletionService completions;
    private final ActorAccess actors;

    @PostMapping("/scenario-versions/{id}/enrollments")
    public ApiResponse<?> enroll(Principal p, @PathVariable String id) {
        return ApiResponse.success(enrollments.enroll(actors.student(p).id(), CoreValues.id(id)));
    }

    @GetMapping("/enrollments")
    public ApiResponse<?> list(
            Principal p,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(
                enrollments.list(
                        actors.student(p).id(), cursor == null ? 0 : CoreValues.id(cursor), limit));
    }

    @GetMapping("/enrollments/{id}")
    public ApiResponse<?> read(Principal p, @PathVariable String id) {
        return ApiResponse.success(enrollments.read(actors.student(p).id(), CoreValues.id(id)));
    }

    @GetMapping("/enrollments/{eid}/tasks/{tid}/submission")
    public ApiResponse<?> submission(
            Principal p, @PathVariable String eid, @PathVariable String tid) {
        return ApiResponse.success(
                submissions.read(actors.student(p).id(), CoreValues.id(eid), CoreValues.id(tid)));
    }

    @PutMapping("/enrollments/{eid}/tasks/{tid}/submission")
    public ApiResponse<?> save(
            Principal p,
            @PathVariable String eid,
            @PathVariable String tid,
            @RequestHeader(value = "If-Match", required = false) String rev,
            @Valid @RequestBody Items r) {
        return ApiResponse.success(
                submissions.save(
                        actors.student(p).id(),
                        CoreValues.id(eid),
                        CoreValues.id(tid),
                        CoreValues.revision(rev),
                        r));
    }

    @PostMapping("/enrollments/{eid}/tasks/{tid}/submission/finalize")
    public ApiResponse<?> finalizeTask(
            Principal p,
            @PathVariable String eid,
            @PathVariable String tid,
            @RequestHeader(value = "If-Match", required = false) String rev,
            @Valid @RequestBody Finalize r) {
        return ApiResponse.success(
                submissions.finalizeTask(
                        actors.student(p).id(),
                        CoreValues.id(eid),
                        CoreValues.id(tid),
                        CoreValues.revision(rev),
                        r));
    }

    @GetMapping("/enrollments/{eid}/tasks/{tid}/model-answer")
    public ApiResponse<?> answer(Principal p, @PathVariable String eid, @PathVariable String tid) {
        return ApiResponse.success(
                answers.readAnswer(actors.student(p).id(), CoreValues.id(eid), CoreValues.id(tid)));
    }

    @GetMapping("/completions/{id}")
    public ApiResponse<?> completion(Principal p, @PathVariable String id) {
        return ApiResponse.success(completions.read(actors.student(p).id(), CoreValues.id(id)));
    }

    @GetMapping("/completions/{id}/resume-snippet")
    public ApiResponse<?> snippet(Principal p, @PathVariable String id) {
        var c = completions.read(actors.student(p).id(), CoreValues.id(id));
        return ApiResponse.success(
                Map.of(
                        "text",
                        c.resumeSnippetText(),
                        "templateVersion",
                        c.snippetTemplateVersion()));
    }

    @GetMapping("/completions/{id}/certificate")
    public ResponseEntity<String> certificate(Principal p, @PathVariable String id) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(completions.renderCertificate(actors.student(p).id(), CoreValues.id(id)));
    }
}
