package com.odc.operationservice.service;

import com.odc.common.constant.UpdateRequestStatus;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.dto.SortRequest;
import com.odc.common.exception.BusinessException;
import com.odc.common.specification.GenericSpecification;
import com.odc.operationservice.dto.request.CreateUpdateRequestRequest;
import com.odc.operationservice.dto.response.GetUpdateRequestResponse;
import com.odc.operationservice.entity.UpdateRequest;
import com.odc.operationservice.repository.UpdateRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateRequestServiceImpl implements UpdateRequestService {
    private final UpdateRequestRepository updateRequestRepository;

    @Override
    public ApiResponse<Void> sendUpdateRequest(CreateUpdateRequestRequest request) {
        UUID requesterId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String code = updateRequestRepository.generateNextCode();

        UpdateRequest entity = UpdateRequest.builder()
                .code(code)
                .requestType(request.getRequestType().toString())
                .targetId(request.getTargetId())
                .changeData(request.getChangeData())
                .status(UpdateRequestStatus.PENDING)
                .requestedBy(requesterId)
                .requestedAt(LocalDateTime.now())
                .build();

        updateRequestRepository.save(entity);
        return ApiResponse.success("Đã gửi yêu cầu cập nhật thành công", null);
    }

    public ApiResponse<GetUpdateRequestResponse> getDetail(UUID id) {
        UpdateRequest entity = updateRequestRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException("Không tìm thấy yêu cầu cập nhật")
                );

        return ApiResponse.success(toDetail(entity));
    }

    public ApiResponse<PaginatedResult<GetUpdateRequestResponse>> searchUpdateRequests(
            SearchRequest request
    ) {

        Specification<UpdateRequest> specification =
                new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(
                        sortRequest.getDirection(),
                        sortRequest.getKey()
                ));
            }
        }

        Sort sort = orders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "requestedAt")
                : Sort.by(orders);

        /* ===== PAGING ===== */
        int page = request.getPage() != null ? request.getPage() - 1 : 0;
        int size = request.getSize() != null ? request.getSize() : 10;

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<GetUpdateRequestResponse> resultPage = updateRequestRepository
                .findAll(specification, pageable)
                .map(this::mapToResponse);

        return ApiResponse.success(PaginatedResult.from(resultPage));
    }

    // Helper functions
    private GetUpdateRequestResponse mapToResponse(UpdateRequest e) {
        return GetUpdateRequestResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .requestType(e.getRequestType())
                .targetId(e.getTargetId())
                .status(e.getStatus())
                .requestedBy(e.getRequestedBy())
                .requestedAt(e.getRequestedAt())
                .build();
    }

    public static GetUpdateRequestResponse toDetail(UpdateRequest e) {
        return GetUpdateRequestResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .requestType(e.getRequestType())
                .targetId(e.getTargetId())
                .changeData(e.getChangeData())
                .status(e.getStatus())
                .requestedBy(e.getRequestedBy())
                .requestedAt(e.getRequestedAt())
                .reviewedBy(e.getReviewedBy())
                .reviewedAt(e.getReviewedAt())
                .rejectReason(e.getRejectReason())
                .build();
    }
}
