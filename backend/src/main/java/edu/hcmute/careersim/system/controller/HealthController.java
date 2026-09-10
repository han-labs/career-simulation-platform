package edu.hcmute.careersim.system.controller;

import edu.hcmute.careersim.common.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(
                Map.of(
                        "status", "UP",
                        "service", "career-simulation-platform-backend",
                        "aiMode", "optional-with-required-fallback"));
    }
}
