package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.projectservice.dto.request.AddProjectMemberRequest;
import com.odc.projectservice.entity.MilestoneMember;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.entity.ProjectMilestone;
import com.odc.projectservice.repository.MilestoneMemberRepository;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectMilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilestoneMemberServiceImpl implements MilestoneMemberService {
    private final MilestoneMemberRepository milestoneMemberRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public ApiResponse<Void> addProjectMembers(AddProjectMemberRequest request, Role allowedRole) {

        ProjectMilestone milestone = projectMilestoneRepository.findById(request.getMilestoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy milestone"));

        UUID projectId = milestone.getProject().getId();
        List<UUID> projectMemberIds = request.getProjectMemberIds();

        if (projectMemberIds == null || projectMemberIds.isEmpty()) {
            throw new BusinessException("Danh sách thành viên dự án không được để trống");
        }

        List<ProjectMember> projectMembers = projectMemberRepository.findByIdIn(projectMemberIds);

        Map<UUID, ProjectMember> projectMemberMap = projectMembers.stream()
                .collect(Collectors.toMap(ProjectMember::getId, pm -> pm));

        List<String> errors = new ArrayList<>();

        for (UUID pmId : projectMemberIds) {

            ProjectMember pm = projectMemberMap.get(pmId);

            if (pm == null) {
                errors.add("Không tìm thấy thành viên dự án với ID: " + pmId);
                continue;
            }

            if (!pm.getProject().getId().equals(projectId)) {
                errors.add("Thành viên " + pmId + " không thuộc dự án này");
                continue;
            }

            if (!pm.getRoleInProject().equals(allowedRole.toString())) {
                errors.add("Thành viên " + pmId + " không có vai trò hợp lệ để thêm vào milestone");
            }

            boolean alreadyJoined = milestoneMemberRepository
                    .existsMemberInMilestone(milestone.getId(), pm.getId());

            if (alreadyJoined) {
                errors.add("Thành viên " + pm.getUserId() + " đã tham gia milestone này trước đó");
            }
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }

        List<MilestoneMember> toSave = projectMembers.stream()
                .map(pm -> MilestoneMember.builder()
                        .projectMilestone(milestone)
                        .projectMember(pm)
                        .joinedAt(LocalDateTime.now())
                        .build())
                .toList();

        milestoneMemberRepository.saveAll(toSave);

        return ApiResponse.success("Thêm thành công thành viên vào milestone", null);
    }
}
