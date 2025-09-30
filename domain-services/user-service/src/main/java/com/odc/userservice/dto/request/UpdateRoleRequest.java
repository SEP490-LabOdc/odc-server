package com.odc.userservice.dto.request;

import com.ctc.wstx.dom.WstxDOMWrappingReader;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateRoleRequest {
    @NotNull(message = "Role ID is required")
    private String roleName;
}