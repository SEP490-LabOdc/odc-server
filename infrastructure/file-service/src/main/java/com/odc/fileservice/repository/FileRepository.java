package com.odc.fileservice.repository;

import com.odc.fileservice.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {
    List<FileEntity> findByEntityId(String entityId);
    List<FileEntity> findByFileUrlIn(List<String> urls);
}