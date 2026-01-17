package com.odc.operationservice.dto.request;

import com.odc.common.constant.UpdateRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class CreateUpdateRequestRequest {

    private UpdateRequestType requestType;

    @NotNull
    private UUID targetId;

    @NotNull
    private Map<String, Object> changeData;
}
