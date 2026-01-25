package com.odc.projectservice.service.mapper;

import com.odc.common.constant.Role;
import com.odc.projectservice.dto.response.ClosureDetailView;
import com.odc.projectservice.dto.response.CompanyClosureView;
import com.odc.projectservice.dto.response.LabAdminClosureView;
import com.odc.projectservice.dto.response.MentorClosureView;
import com.odc.projectservice.entity.ProjectClosureRequest;
import com.odc.userservice.v1.UserInfo;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProjectClosureViewMapper {

    public Object toListView(ProjectClosureRequest e, Role role) {
        return toListView(e, role, null);
    }

    public Object toListView(ProjectClosureRequest e, Role role, UserInfo userInfo) {
        return switch (role) {
            case MENTOR -> MentorClosureView.builder()
                    .id(e.getId())
                    .status(e.getStatus())
                    .reason(e.getReason())
                    .summary(e.getSummary())
                    .labAdminComment(e.getLabAdminComment())
                    .companyComment(e.getCompanyComment())
                    .createdAt(e.getCreatedAt())
                    .build();

            case LAB_ADMIN -> LabAdminClosureView.builder()
                    .id(e.getId())
                    .projectId(e.getProject().getId())
                    .projectTitle(e.getProject().getTitle())
                    .createdBy(e.getRequestedBy())
                    .reason(e.getReason())
                    .summary(e.getSummary())
                    .status(e.getStatus())
                    .createdAt(e.getCreatedAt())
                    .createdByName(userInfo.getFullName())
                    .createdByAvatar(userInfo.getAvatarUrl())
                    .build();

            case COMPANY -> CompanyClosureView.builder()
                    .id(e.getId())
                    .projectId(e.getProject().getId())
                    .projectTitle(e.getProject().getTitle())
                    .summary(e.getSummary())
                    .status(e.getStatus())
                    .labAdminComment(e.getLabAdminComment())
                    .labAdminReviewedAt(e.getLabAdminReviewedAt())
                    .createdAt(e.getCreatedAt())
                    .build();

            default -> null;
        };
    }

    public ClosureDetailView toDetail(ProjectClosureRequest e) {
        return ClosureDetailView.builder()
                .id(e.getId())
                .projectId(e.getProject().getId())
                .mentorId(e.getRequestedBy())
                .reason(e.getReason())
                .summary(e.getSummary())
                .status(e.getStatus())
                .labAdminComment(e.getLabAdminComment())
                .labAdminReviewedAt(e.getLabAdminReviewedAt())
                .companyComment(e.getCompanyComment())
                .companyReviewedAt(e.getCompanyReviewedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
