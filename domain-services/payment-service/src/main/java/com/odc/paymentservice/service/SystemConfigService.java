package com.odc.paymentservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.paymentservice.dto.request.SystemConfigRequest;
import com.odc.paymentservice.entity.SystemConfig;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SystemConfigService {

    ApiResponse<SystemConfig> create(SystemConfigRequest request);

    ApiResponse<SystemConfig> update(UUID id, SystemConfigRequest request);

    void delete(UUID id);

    ApiResponse<SystemConfig> getById(UUID id);

    ApiResponse<PaginatedResult<SystemConfig>> getAll(Pageable pageable);
}
