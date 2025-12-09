package com.odc.paymentservice.controller;

import com.odc.common.constant.PaymentConstant;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.paymentservice.dto.request.SystemConfigRequest;
import com.odc.paymentservice.dto.request.UpdateSystemConfigRequest;
import com.odc.paymentservice.entity.SystemConfig;
import com.odc.paymentservice.scheduler.DynamicFeeDistributionScheduler;
import com.odc.paymentservice.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/system-configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService service;
    private final DynamicFeeDistributionScheduler dynamicFeeDistributionScheduler;

    @PostMapping
    public ResponseEntity<ApiResponse<SystemConfig>> create(@RequestBody @Valid SystemConfigRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemConfig>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateSystemConfigRequest request) {

        // 1. Cập nhật cấu hình
        // (Giả định SystemConfigService đã được implement để xử lý Map<String, Object> properties)
        SystemConfig updatedConfig = service.update(id, request);

        // 2. LOGIC KIỂM TRA TÊN VÀ RESET SCHEDULER
        if (PaymentConstant.SYSTEM_CONFIG_FEE_DISTRIBUTION_NAME.equals(updatedConfig.getName()))
            dynamicFeeDistributionScheduler.rescheduleTask();

        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công config", updatedConfig));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemConfig>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResult<SystemConfig>>> getAll(@RequestParam(defaultValue = "1") int page,
                                                                             @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return ResponseEntity.ok(service.getAll(pageable));
    }
}
