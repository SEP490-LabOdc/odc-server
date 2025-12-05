package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.common.specification.GenericSpecification;
import com.odc.projectservice.dto.request.CreateSystemTemplateRequest;
import com.odc.projectservice.dto.request.UpdateSystemTemplateRequest;
import com.odc.projectservice.dto.response.SystemTemplateResponse;
import com.odc.projectservice.entity.SystemTemplate;
import com.odc.projectservice.repository.SystemTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemTemplateServiceImpl implements SystemTemplateService {

    private final SystemTemplateRepository templateRepository;

    @Override
    public ApiResponse<SystemTemplateResponse> createTemplate(CreateSystemTemplateRequest request) {
        if (templateRepository.existsByName(request.getName())) {
            throw new BusinessException("Tên template đã tồn tại: " + request.getName());
        }

        SystemTemplate template = SystemTemplate.builder()
                .name(request.getName())
                .type(request.getType().toUpperCase())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .category(request.getCategory().toUpperCase())
                .description(request.getDescription())
                .build();

        templateRepository.save(template);
        return ApiResponse.success("Tạo template thành công", mapToResponse(template));
    }

    @Override
    public ApiResponse<SystemTemplateResponse> updateTemplate(UUID id, UpdateSystemTemplateRequest request) {
        SystemTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        if (request.getName() != null) template.setName(request.getName());
        if (request.getType() != null) template.setType(request.getType());
        if (request.getFileUrl() != null) template.setFileUrl(request.getFileUrl());
        if (request.getFileName() != null) template.setFileName(request.getFileName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getCategory() != null) template.setCategory(request.getCategory());

        templateRepository.save(template);
        return ApiResponse.success("Cập nhật template thành công", mapToResponse(template));
    }

    @Override
    public ApiResponse<Void> deleteTemplate(UUID id) {
        SystemTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        template.setIsDeleted(true);
        templateRepository.save(template);

        return ApiResponse.success("Xóa template thành công", null);
    }

    @Override
    public ApiResponse<PaginatedResult<SystemTemplateResponse>> searchTemplates(SearchRequest request) {
        Specification<SystemTemplate> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (com.odc.common.dto.SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        } else {
            orders.add(new Sort.Order(Sort.Direction.DESC, "updatedAt"));
        }
        Sort sort = Sort.by(orders);

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);
        Page<SystemTemplateResponse> page = templateRepository.findAll(specification, pageable)
                .map(this::mapToResponse);

        return ApiResponse.success(PaginatedResult.from(page));
    }

    @Override
    public ApiResponse<SystemTemplateResponse> getLatestTemplateByType(String type) {
        SystemTemplate template = templateRepository.findFirstByTypeAndIsDeletedFalseOrderByUpdatedAtDesc(type)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có mẫu nào cho loại: " + type));

        return ApiResponse.success(mapToResponse(template));
    }

    private SystemTemplateResponse mapToResponse(SystemTemplate entity) {
        return SystemTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .category(entity.getCategory())
                .fileUrl(entity.getFileUrl())
                .fileName(entity.getFileName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}