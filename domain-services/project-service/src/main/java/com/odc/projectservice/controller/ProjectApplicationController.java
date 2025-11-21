package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.ApplyProjectRequest;
import com.odc.projectservice.dto.response.ApplyProjectResponse;
import com.odc.projectservice.dto.response.UserSubmittedCvResponse;
import com.odc.projectservice.service.ProjectApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project-applications")
@RequiredArgsConstructor
public class ProjectApplicationController {
    private final ProjectApplicationService projectApplicationService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ApplyProjectResponse>> applyProject(
            @Valid @RequestBody ApplyProjectRequest request
    ) {
        return ResponseEntity.ok(projectApplicationService.applyProject(request));
    }

    @GetMapping("/my-submitted-cvs")
    public ResponseEntity<ApiResponse<List<UserSubmittedCvResponse>>> getMySubmittedCvs() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ApiResponse<List<UserSubmittedCvResponse>> response = projectApplicationService.getUserSubmittedCvs(userId);
        return ResponseEntity.ok(response);
    }
}
