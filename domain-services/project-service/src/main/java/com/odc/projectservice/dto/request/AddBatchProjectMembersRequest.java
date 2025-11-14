package com.odc.projectservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AddBatchProjectMembersRequest {
    @NotNull(message = "projectId không được bỏ trống")
    private UUID projectId;

    @NotEmpty(message = "Danh sách userIds không được rỗng")
    private List<UUID> userIds;
}
