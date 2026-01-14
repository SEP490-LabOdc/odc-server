package com.odc.operationservice.dto.response;

import com.odc.common.constant.UpdateRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UpdateRequestListItemResponse {

    private UUID id;

    private String code;

    private String requestType;

    private UUID targetId;

    private UpdateRequestStatus status;

    private UUID requestedBy;

    private LocalDateTime requestedAt;
}
