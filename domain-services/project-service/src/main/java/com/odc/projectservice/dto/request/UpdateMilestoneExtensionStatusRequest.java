package com.odc.projectservice.dto.request;

import com.odc.common.constant.MilestoneExtensionRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMilestoneExtensionStatusRequest {
    private String reason;

    @NotNull(message = "Không được để trống trạng thái")
    private MilestoneExtensionRequestStatus status;
}
