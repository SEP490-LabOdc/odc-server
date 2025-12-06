package com.odc.projectservice.service;

import com.odc.common.constant.Role;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.projectservice.dto.request.AddProjectMemberRequest;
import com.odc.projectservice.dto.request.RemoveMilestoneMembersRequest;
import com.odc.projectservice.dto.request.UpdateMilestoneMemberRoleRequest;
import com.odc.projectservice.dto.response.GetMilestoneMember;
import com.odc.projectservice.dto.response.GetMilestoneMemberResponse;
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy milestone với ID: " + request.getMilestoneId()));

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
                errors.add("Không tìm thấy thành viên với projectMemberId: " + pmId);
                continue;
            }

            if (!pm.getProject().getId().equals(projectId)) {
                errors.add("Thành viên với projectMemberId " + pmId + " không thuộc dự án này");
                continue;
            }
            if (!pm.getRoleInProject().equalsIgnoreCase(allowedRole.toString())) {
                errors.add("Thành viên với projectMemberId " + pmId + " không có vai trò " + allowedRole + " để thêm vào milestone");
            }

            MilestoneMember existing = milestoneMemberRepository
                    .findByProjectMilestone_IdAndProjectMember_Id(milestone.getId(), pm.getId())
                    .orElse(null);

            if (existing != null) {
                if (existing.isActive()) {
                    errors.add("Thành viên với userId " + pm.getUserId() + " đã tham gia milestone này trước đó");
                    continue;
                }

                existing.setActive(true);
                existing.setJoinedAt(LocalDateTime.now());
                existing.setLeftAt(null);
                milestoneMemberRepository.save(existing);
            } else {
                MilestoneMember mm = MilestoneMember.builder()
                        .projectMilestone(milestone)
                        .projectMember(pm)
                        .joinedAt(LocalDateTime.now())
                        .isActive(true)
                        .build();
                milestoneMemberRepository.save(mm);
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
                        .isActive(true)
                        .build())
                .toList();

        milestoneMemberRepository.saveAll(toSave);

        return ApiResponse.success("Thêm thành công các thành viên vào milestone", null);
    }

    @Override
    public ApiResponse<Void> removeProjectMembersFromMilestone(RemoveMilestoneMembersRequest request) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(request.getMilestoneId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy milestone với ID: " + request.getMilestoneId()));

        List<UUID> projectMemberIds = request.getProjectMemberIds();
        if (projectMemberIds == null || projectMemberIds.isEmpty()) {
            throw new BusinessException("Danh sách thành viên để loại bỏ không được để trống");
        }

        List<MilestoneMember> milestoneMembers = milestoneMemberRepository
                .findByProjectMilestoneIdAndProjectMemberIds(milestone.getId(), projectMemberIds);

        if (milestoneMembers.isEmpty()) {
            throw new BusinessException("Không tìm thấy các thành viên trong milestone này");
        }

        List<String> errors = new ArrayList<>();

        for (MilestoneMember mm : milestoneMembers) {
            ProjectMember pm = mm.getProjectMember();

            if (!Role.TALENT.toString().equalsIgnoreCase(pm.getRoleInProject())) {
                errors.add("Chỉ TALENT mới có thể bị loại bỏ, thành viên với projectMemberId: " + pm.getId());
                continue;
            }

            if (!mm.isActive()) {
                errors.add("Thành viên với projectMemberId " + pm.getId() + " đã rời milestone trước đó");
                continue;
            }

            mm.setLeftAt(LocalDateTime.now());
            mm.setActive(true);
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }

        milestoneMemberRepository.saveAll(milestoneMembers);

        return ApiResponse.success("Loại bỏ thành viên khỏi milestone thành công", null);
    }

    @Override
    public ApiResponse<GetMilestoneMemberResponse> getMilestoneMembers(UUID milestoneId, Boolean isActive) {
        List<MilestoneMember> milestoneMembers;

        if (isActive == null) {
            milestoneMembers = milestoneMemberRepository.findByProjectMilestone_Id(milestoneId);
        } else if (isActive) {
            milestoneMembers = milestoneMemberRepository.findByProjectMilestone_IdAndIsActive(milestoneId, true);
        } else {
            milestoneMembers = milestoneMemberRepository.findByProjectMilestone_IdAndIsActive(milestoneId, false);
        }

        if (milestoneMembers.isEmpty()) {
            return ApiResponse.success(new GetMilestoneMemberResponse());
        }

        List<String> userIds = milestoneMembers.stream()
                .map(mm -> mm.getProjectMember().getUserId().toString())
                .toList();

        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(userServiceChannel);
        GetUsersByIdsResponse usersResponse = stub.getUsersByIds(
                GetUsersByIdsRequest.newBuilder().addAllUserId(userIds).build()
        );

        Map<UUID, UserInfo> userMap = usersResponse.getUsersList().stream()
                .collect(Collectors.toMap(
                        u -> UUID.fromString(u.getUserId()),
                        u -> u
                ));

        List<GetMilestoneMember> talents = new ArrayList<>();
        List<GetMilestoneMember> mentors = new ArrayList<>();

        for (MilestoneMember mm : milestoneMembers) {
            ProjectMember pm = mm.getProjectMember();
            UserInfo userInfo = userMap.get(pm.getUserId());

            GetMilestoneMember dto = new GetMilestoneMember();
            dto.setMilestoneMemberId(mm.getId());
            dto.setProjectMemberId(pm.getId());
            dto.setUserId(pm.getUserId());
            dto.setJoinedAt(mm.getJoinedAt());
            dto.setLeftAt(mm.getLeftAt());
            dto.setLeader(mm.isLeader());

            if (userInfo != null) {
                dto.setFullName(userInfo.getFullName());
                dto.setEmail(userInfo.getEmail());
                dto.setPhone(userInfo.getPhone());
                dto.setAvatarUrl(userInfo.getAvatarUrl());
            }


            if (Role.TALENT.toString().equalsIgnoreCase(pm.getRoleInProject())) {
                talents.add(dto);
            } else if (Role.MENTOR.toString().equalsIgnoreCase(pm.getRoleInProject())) {
                mentors.add(dto);
            }
        }

        GetMilestoneMemberResponse response = new GetMilestoneMemberResponse();
        response.setTalents(talents);
        response.setMentors(mentors);

        return ApiResponse.success(response);
    }

    @Override
    public ApiResponse<List<GetMilestoneMember>> getMilestoneMembers(UUID milestoneId, Boolean isActive, String role) {
        List<MilestoneMember> milestoneMembers;

        // 1. Lấy dữ liệu thô từ DB
        if (isActive == null) {
            milestoneMembers = milestoneMemberRepository.findByProjectMilestone_Id(milestoneId);
        } else {
            milestoneMembers = milestoneMemberRepository.findByProjectMilestone_IdAndIsActive(milestoneId, isActive);
        }

        if (milestoneMembers.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        // 2. Lấy thông tin user từ Identity Service (gRPC)
        List<String> userIds = milestoneMembers.stream()
                .map(mm -> mm.getProjectMember().getUserId().toString())
                .toList();

        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(userServiceChannel);
        GetUsersByIdsResponse usersResponse = stub.getUsersByIds(
                GetUsersByIdsRequest.newBuilder().addAllUserId(userIds).build()
        );

        Map<UUID, UserInfo> userMap = usersResponse.getUsersList().stream()
                .collect(Collectors.toMap(
                        u -> UUID.fromString(u.getUserId()),
                        u -> u
                ));

        // 3. Map sang DTO và lọc theo Role
        List<GetMilestoneMember> result = new ArrayList<>();

        for (MilestoneMember mm : milestoneMembers) {
            ProjectMember pm = mm.getProjectMember();

            // Logic lọc theo Role (nếu có param role)
            if (role != null && !role.trim().isEmpty()) {
                if (!role.equalsIgnoreCase(pm.getRoleInProject())) {
                    continue; // Bỏ qua nếu không đúng role
                }
            }

            UserInfo userInfo = userMap.get(pm.getUserId());

            GetMilestoneMember dto = new GetMilestoneMember();
            dto.setMilestoneMemberId(mm.getId());
            dto.setProjectMemberId(pm.getId());
            dto.setUserId(pm.getUserId());
            dto.setJoinedAt(mm.getJoinedAt());
            dto.setLeftAt(mm.getLeftAt());
            dto.setLeader(mm.isLeader());

            // Nếu cần hiển thị role trong response để client biết,
            // bạn nên thêm field 'role' vào GetMilestoneMember DTO.
            // Tạm thời code này dùng DTO hiện tại.

            if (userInfo != null) {
                dto.setFullName(userInfo.getFullName());
                dto.setEmail(userInfo.getEmail());
                dto.setPhone(userInfo.getPhone());
                dto.setAvatarUrl(userInfo.getAvatarUrl());
            }

            result.add(dto);
        }

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<Void> updateMilestoneMemberRole(UUID milestoneId, UUID milestoneMemberId, UpdateMilestoneMemberRoleRequest request) {
        MilestoneMember member = milestoneMemberRepository
                .findByProjectMilestone_IdAndIdAndIsActive(milestoneId, milestoneMemberId, true)
                .orElseThrow(() -> new BusinessException("Thành viên không thuộc milestone"));

        if (request.isLeader()) {

            // 3. Check milestone đã có leader chưa
            boolean leaderExists = milestoneMemberRepository
                    .existsByProjectMilestone_IdAndIsLeaderTrue(milestoneId);

            // 4. Nếu đã có leader mà user hiện không phải leader → throw lỗi
            if (leaderExists && !member.isLeader()) {
                throw new BusinessException("Milestone đã có leader khác");
            }

            // 5. Nếu không ai là leader hoặc chính user muốn giữ leader → cho phép set
            member.setLeader(true);
        } else {
            // Nếu request muốn set MEMBER (isLeader = false)
            member.setLeader(false);
        }

        milestoneMemberRepository.save(member);
        return ApiResponse.success(null);
    }
}
