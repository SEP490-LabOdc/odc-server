package com.odc.fileservice.service;

import com.odc.common.constant.ApiConstants;
import com.odc.common.exception.BusinessException;
import com.odc.fileservice.entity.FileEntity;
import com.odc.fileservice.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final FileRepository fileRepository;

    public FileEntity uploadFile(MultipartFile file, String entityId) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("File rỗng", ApiConstants.VALIDATION_ERROR);
        }
        String originalFileName = file.getOriginalFilename();
        String s3Key = "uploads/" + UUID.randomUUID().toString() + "_" + originalFileName;

        String fileUrl = s3Service.uploadFile(s3Key, file);

        FileEntity fileEntity = FileEntity.builder()
                .fileName(originalFileName)
                .fileUrl(fileUrl)
                .s3Key(s3Key)
                .uploadedAt(LocalDateTime.now())
                .build();

        if(entityId != null && !entityId.isEmpty()) {
            fileEntity.setEntityId(entityId);
        }

        return fileRepository.save(fileEntity);
    }

    public List<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }

    public void deleteFile(UUID id) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy file với Id: " + id));

        // Xóa file trên S3
        s3Service.deleteFile(fileEntity.getS3Key());

        // Xóa thông tin file trong database
        fileRepository.delete(fileEntity);
    }

    public List<FileEntity> getFilesByEntityId(String entityId) {
        return fileRepository.findByEntityId(entityId);
    }
}