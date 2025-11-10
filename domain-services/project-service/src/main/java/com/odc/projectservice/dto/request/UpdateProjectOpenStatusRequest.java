package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateProjectOpenStatusRequest {

    @NotNull(message = "Trạng thái mở tuyển thành viên không được để trống.")
    private Boolean isOpenForApplications;
}
