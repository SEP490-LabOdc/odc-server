package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.CreateProjectDocumentRequest;
import com.odc.projectservice.dto.request.UpdateProjectDocumentRequest;
import com.odc.projectservice.dto.response.ProjectDocumentResponse;
import com.odc.projectservice.service.ProjectDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/project-documents")
@RequiredArgsConstructor
public class ProjectDocumentController {
    private final ProjectDocumentService projectDocumentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDocumentResponse>> createProjectDocument(
            @Valid @RequestBody CreateProjectDocumentRequest request) {
        ApiResponse<ProjectDocumentResponse> response = projectDocumentService.createProjectDocument(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<ApiResponse<ProjectDocumentResponse>> updateProjectDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody UpdateProjectDocumentRequest request) {
        ApiResponse<ProjectDocumentResponse> response = projectDocumentService.updateProjectDocument(documentId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteProjectDocument(@PathVariable UUID documentId) {
        ApiResponse<Void> response = projectDocumentService.deleteProjectDocument(documentId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<ProjectDocumentResponse>> getProjectDocumentById(@PathVariable UUID documentId) {
        ApiResponse<ProjectDocumentResponse> response = projectDocumentService.getProjectDocumentById(documentId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDocumentResponse>>> getAllProjectDocuments() {
        ApiResponse<List<ProjectDocumentResponse>> response = projectDocumentService.getAllProjectDocuments();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
