package com.odc.projectservice.repository;

import com.odc.projectservice.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectId(UUID projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.roleInProject = 'MENTOR' AND pm.userId IN :userIds")
    List<ProjectMember> findMentorMembersByUserIds(List<UUID> userIds);

}
