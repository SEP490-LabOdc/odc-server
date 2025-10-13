package com.odc.companyservice.dto.request;

import com.odc.common.constant.Status;
import lombok.Getter;

@Getter
public class UpdateRegisterStatusRequest {
    private Status status;
}
