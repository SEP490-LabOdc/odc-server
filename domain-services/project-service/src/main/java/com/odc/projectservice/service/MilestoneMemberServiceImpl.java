package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.projectservice.dto.request.AddProjectMemberRequest;
import com.odc.projectservice.dto.request.RemoveMilestoneMembersRequest;
import com.odc.projectservice.dto.response.TalentInMilestoneResponse;
import com.odc.projectservice.entity.MilestoneMember;
import com.odc.projectservice.entity.ProjectMember;
import com.odc.projectservice.entity.ProjectMilestone;
import com.odc.projectservice.repository.MilestoneMemberRepository;
import com.odc.projectservice.repository.ProjectMemberRepository;
import com.odc.projectservice.repository.ProjectMilestoneRepository;
import com.odc.userservice.v1.GetUsersByIdsRequest;
import com.odc.userservice.v1.GetUsersByIdsResponse;
import com.odc.userservice.v1.UserInfo;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MilestoneMemberServiceImpl implements MilestoneMemberService {
    private final MilestoneMemberRepository milestoneMemberRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ManagedChannel userServiceChannel;

    public MilestoneMemberServiceImpl(
            MilestoneMemberRepository milestoneMemberRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectMilestoneRepository projectMilestoneRepository,
            @Qualifier("userServiceChannel1") ManagedChannel userServiceChannel) {
        this.milestoneMemberRepository = milestoneMemberRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userServiceChannel = userServiceChannel;
        this.projectMilestoneRepository = projectMilestoneRepository;
    }

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

    @Override
    public ApiResponse<Void> removeProjectMembersFromMilestone(RemoveMilestoneMembersRequest request) {

        ProjectMilestone milestone = projectMilestoneRepository.findById(request.getMilestoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy milestone"));

        List<UUID> projectMemberIds = request.getProjectMemberIds();
        if (projectMemberIds == null || projectMemberIds.isEmpty()) {
            throw new BusinessException("Danh sách thành viên không được để trống");
        }

        List<MilestoneMember> milestoneMembers = milestoneMemberRepository
                .findByProjectMilestoneIdAndProjectMemberIds(milestone.getId(), projectMemberIds);

        if (milestoneMembers.isEmpty()) {
            throw new BusinessException("Không tìm thấy thành viên trong milestone này");
        }

        List<String> errors = new ArrayList<>();

        for (MilestoneMember mm : milestoneMembers) {

            ProjectMember pm = mm.getProjectMember();

            if (!Role.TALENT.toString().equalsIgnoreCase(pm.getRoleInProject())) {
                errors.add("Chỉ TALENT mới có thể bị kick khỏi milestone, thành viên: " + pm.getUserId());
                continue;
            }

            if (mm.getLeftAt() != null) {
                errors.add("Thành viên " + pm.getUserId() + " đã rời milestone trước đó");
                continue;
            }

            mm.setLeftAt(LocalDateTime.now());
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }

        milestoneMemberRepository.saveAll(milestoneMembers);

        return ApiResponse.success("Loại bỏ thành viên khỏi milestone thành công", null);
    }

    @Override
    public ApiResponse<List<TalentInMilestoneResponse>> getActiveTalentsInMilestone(UUID milestoneId) {
        List<MilestoneMember> milestoneMembers = milestoneMemberRepository
                .findByProjectMilestone_IdAndProjectMember_RoleInProjectAndLeftAtIsNull(
                        milestoneId, "TALENT");        if (milestoneMembers.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        List<String> userIds = milestoneMembers.stream()
                .map(mm -> mm.getProjectMember().getUserId().toString())
                .toList();

        UserServiceGrpc.UserServiceBlockingStub stub =
                UserServiceGrpc.newBlockingStub(userServiceChannel);

        GetUsersByIdsResponse usersResponse = stub.getUsersByIds(
                GetUsersByIdsRequest.newBuilder()
                        .addAllUserId(userIds)
                        .build()
        );

        Map<UUID, UserInfo> userMap = usersResponse.getUsersList().stream()
                .collect(Collectors.toMap(
                        u -> UUID.fromString(u.getUserId()),
                        u -> u
                ));

        List<TalentInMilestoneResponse> result = milestoneMembers.stream().map(mm -> {
            ProjectMember pm = mm.getProjectMember();
            UserInfo userInfo = userMap.get(pm.getUserId());

            TalentInMilestoneResponse dto = new TalentInMilestoneResponse();
            dto.setProjectMemberId(pm.getId());
            dto.setUserId(pm.getUserId());

            if (userInfo != null) {
                dto.setFullName(userInfo.getFullName());
                dto.setEmail(userInfo.getEmail());
                dto.setPhone(userInfo.getPhone());
                dto.setAvatarUrl(userInfo.getAvatarUrl());
            }

            dto.setJoinedAt(mm.getJoinedAt());

            return dto;
        }).toList();

        return ApiResponse.success(result);
    }
}
