package com.odc.operationservice.dto.response;

import com.odc.common.constant.UpdateRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class GetUpdateRequestResponse {

    private UUID id;

    private String code;

    private String requestType;

    private UUID targetId;

    private Map<String, Object> changeData;

    private UpdateRequestStatus status;

    private UUID requestedBy;

    private LocalDateTime requestedAt;

    private UUID reviewedBy;

    private LocalDateTime reviewedAt;

    private String rejectReason;
}
