package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.dto.SortRequest;
import com.odc.common.exception.BusinessException;
import com.odc.common.specification.GenericSpecification;
import com.odc.projectservice.dto.request.CreateProjectDocumentRequest;
import com.odc.projectservice.dto.request.UpdateProjectDocumentRequest;
import com.odc.projectservice.dto.response.ProjectDocumentResponse;
import com.odc.projectservice.entity.Project;
import com.odc.projectservice.entity.ProjectDocument;
import com.odc.projectservice.repository.ProjectDocumentRepository;
import com.odc.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectDocumentServiceImpl implements ProjectDocumentService {
    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectRepository projectRepository;

    @Override
    public ApiResponse<ProjectDocumentResponse> createProjectDocument(CreateProjectDocumentRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("Dự án với ID '" + request.getProjectId() + "' không tồn tại"));

        ProjectDocument projectDocument = ProjectDocument.builder()
                .documentName(request.getDocumentName())
                .documentUrl(request.getDocumentUrl())
                .documentType(request.getDocumentType())
                .uploadedAt(LocalDateTime.now())
                .project(project)
                .build();

        ProjectDocument savedDocument = projectDocumentRepository.save(projectDocument);

        ProjectDocumentResponse responseData = ProjectDocumentResponse.builder()
                .id(savedDocument.getId())
                .projectId(savedDocument.getProject().getId())
                .documentName(savedDocument.getDocumentName())
                .documentUrl(savedDocument.getDocumentUrl())
                .documentType(savedDocument.getDocumentType())
                .uploadedAt(savedDocument.getUploadedAt())
                .build();
        return ApiResponse.success("Tạo tài liệu dự án thành công", responseData);
    }

    @Override
    public ApiResponse<ProjectDocumentResponse> updateProjectDocument(UUID documentId, UpdateProjectDocumentRequest request) {
        ProjectDocument existingDocument = projectDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Tài liệu dự án với ID '" + documentId + "' không tồn tại"));

        existingDocument.setDocumentName(request.getDocumentName());
        existingDocument.setDocumentUrl(request.getDocumentUrl());
        existingDocument.setDocumentType(request.getDocumentType());

        ProjectDocument updatedDocument = projectDocumentRepository.save(existingDocument);

        ProjectDocumentResponse responseData = ProjectDocumentResponse.builder()
                .id(updatedDocument.getId())
                .projectId(updatedDocument.getProject().getId())
                .documentName(updatedDocument.getDocumentName())
                .documentUrl(updatedDocument.getDocumentUrl())
                .documentType(updatedDocument.getDocumentType())
                .uploadedAt(updatedDocument.getUploadedAt())
                .build();
        return ApiResponse.success("Cập nhật tài liệu dự án thành công", responseData);
    }

    @Override
    public ApiResponse<Void> deleteProjectDocument(UUID documentId) {
        ProjectDocument existingDocument = projectDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Tài liệu dự án với ID '" + documentId + "' không tồn tại"));

        projectDocumentRepository.delete(existingDocument);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa tài liệu dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(null)
                .build();
    }

    @Override
    public ApiResponse<ProjectDocumentResponse> getProjectDocumentById(UUID documentId) {
        ProjectDocument document = projectDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("Tài liệu dự án với ID '" + documentId + "' không tồn tại"));

        ProjectDocumentResponse responseData = ProjectDocumentResponse.builder()
                .id(document.getId())
                .projectId(document.getProject().getId())
                .documentName(document.getDocumentName())
                .documentUrl(document.getDocumentUrl())
                .documentType(document.getDocumentType())
                .uploadedAt(document.getUploadedAt())
                .build();

        return ApiResponse.success("Lấy thông tin tài liệu dự án thành công", responseData);
    }

    @Override
    public ApiResponse<List<ProjectDocumentResponse>> getAllProjectDocuments() {
        List<ProjectDocumentResponse> documents = projectDocumentRepository.findAll()
                .stream()
                .map(document -> ProjectDocumentResponse.builder()
                        .id(document.getId())
                        .projectId(document.getProject().getId())
                        .documentName(document.getDocumentName())
                        .documentUrl(document.getDocumentUrl())
                        .documentType(document.getDocumentType())
                        .uploadedAt(document.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách tài liệu dự án thành công", documents);
    }

    @Override
    public ApiResponse<List<ProjectDocumentResponse>> searchProjectDocuments(SearchRequest request) {
        Specification<ProjectDocument> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);

        List<ProjectDocumentResponse> documents = projectDocumentRepository.findAll(specification, sort)
                .stream()
                .map(document -> ProjectDocumentResponse.builder()
                        .id(document.getId())
                        .projectId(document.getProject().getId())
                        .documentName(document.getDocumentName())
                        .documentUrl(document.getDocumentUrl())
                        .documentType(document.getDocumentType())
                        .uploadedAt(document.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.<List<ProjectDocumentResponse>>builder()
                .success(true)
                .message("Tìm kiếm tài liệu dự án thành công")
                .timestamp(LocalDateTime.now())
                .data(documents)
                .build();
    }

    @Override
    public ApiResponse<PaginatedResult<ProjectDocumentResponse>> searchProjectDocumentsWithPagination(SearchRequest request) {
        Specification<ProjectDocument> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        Page<ProjectDocumentResponse> page = projectDocumentRepository.findAll(specification, pageable)
                .map(document -> ProjectDocumentResponse.builder()
                        .id(document.getId())
                        .projectId(document.getProject().getId())
                        .documentName(document.getDocumentName())
                        .documentUrl(document.getDocumentUrl())
                        .documentType(document.getDocumentType())
                        .uploadedAt(document.getUploadedAt())
                        .build());

        return ApiResponse.success(PaginatedResult.from(page));
    }
}