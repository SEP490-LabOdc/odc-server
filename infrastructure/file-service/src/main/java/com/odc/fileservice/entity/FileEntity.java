package com.odc.fileservice.entity;

import com.odc.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "files")
public class FileEntity extends BaseEntity {


    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false)
    private String fileUrl; // URL công khai để truy cập file

    @Column(name = "s3_key", nullable = false)
    private String s3Key; // Key của file trên S3 (dùng để xóa)

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}