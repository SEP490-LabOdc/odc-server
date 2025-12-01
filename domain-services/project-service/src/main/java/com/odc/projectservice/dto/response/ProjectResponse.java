package com.odc.projectservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ProjectResponse {
    private UUID id;
    private UUID companyId;
    private UUID mentorId;
    private String title;
    private String description;
    private String status;
    private Boolean isOpenForApplications;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal budget;
    private Set<SkillResponse> skills;
    private List<UserParticipantResponse> mentors;
    private List<UserParticipantResponse> talents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UUID createdBy;
    private String createdByName;
    private String createdByAvatar;
    private UUID currentMilestoneId;
    private String currentMilestoneName;
    private String companyName;
}
