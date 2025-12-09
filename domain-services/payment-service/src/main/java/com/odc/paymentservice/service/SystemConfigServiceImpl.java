package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.exception.BusinessException;
import com.odc.paymentservice.dto.request.SystemConfigRequest;
import com.odc.paymentservice.dto.request.UpdateSystemConfigRequest;
import com.odc.paymentservice.entity.SystemConfig;
import com.odc.paymentservice.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {
    private final SystemConfigRepository systemConfigRepository;

    @Override
    public ApiResponse<SystemConfig> create(SystemConfigRequest request) {
        systemConfigRepository.findByName(request.getName()).ifPresent(c -> {
            throw new IllegalArgumentException("Config với name này đã tồn tại");
        });

        SystemConfig config = SystemConfig.builder()
                .name(request.getName())
                .description(request.getDescription())
                .properties(request.getProperties())
                .build();
        return ApiResponse.success("Thêm thành công config", systemConfigRepository.save(config));
    }

    @Override
    public SystemConfig update(UUID id, UpdateSystemConfigRequest request) {
        SystemConfig config = systemConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Config không tồn tại"));

        config.setDescription(request.getDescription());
        config.setProperties(request.getProperties());
        return systemConfigRepository.save(config);
    }

    @Override
    public void delete(UUID id) {
        systemConfigRepository.deleteById(id);
    }

    @Override
    public ApiResponse<SystemConfig> getById(UUID id) {
        return ApiResponse.success(systemConfigRepository.findById(id).orElse(null));
    }

    @Override
    public ApiResponse<PaginatedResult<SystemConfig>> getAll(Pageable pageable) {
        Page<SystemConfig> page = systemConfigRepository.findAll(pageable);
        return ApiResponse.success(PaginatedResult.from(page));
    }
}
