package com.odc.projectservice.repository;

import com.odc.projectservice.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID>, JpaSpecificationExecutor<Skill> {
    Optional<Skill> findByName(String name);

    boolean existsByName(String name);
}
