package com.odc.operationservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.operationservice.dto.request.CreateUpdateRequestRequest;
import com.odc.operationservice.dto.response.GetUpdateRequestResponse;

import java.util.UUID;

public interface UpdateRequestService {
    ApiResponse<GetUpdateRequestResponse> getDetail(UUID id);

    ApiResponse<PaginatedResult<GetUpdateRequestResponse>> searchUpdateRequests(SearchRequest request);

    ApiResponse<Void> sendUpdateRequest(CreateUpdateRequestRequest request);
}
