package com.odc.projectservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddMilestoneAttachmentsRequest {
    @NotEmpty(message = "Danh sách attachments không được rỗng")
    @Valid
    private List<CreateMilestoneAttachmentRequest> attachments;
}