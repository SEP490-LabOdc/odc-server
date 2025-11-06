package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.ApplyProjectRequest;
import com.odc.projectservice.dto.response.ApplyProjectResponse;
import com.odc.projectservice.service.ProjectApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
