package com.odc.fileservice.service;

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

    public FileEntity uploadFile(MultipartFile file) throws IOException {
        try {
            System.out.println("=== Upload File Debug ===");
            System.out.println("Original filename: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());
            System.out.println("Is empty: " + file.isEmpty());

            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            String originalFileName = file.getOriginalFilename();
            String s3Key = "uploads/" + UUID.randomUUID().toString() + "-" + originalFileName;

            System.out.println("S3 Key: " + s3Key);

            // Upload file lên S3 và lấy URL
            String fileUrl = s3Service.uploadFile(s3Key, file);
            System.out.println("S3 URL: " + fileUrl);

            // Lưu thông tin file vào database
            FileEntity fileEntity = FileEntity.builder()
                    .fileName(originalFileName)
                    .fileUrl(fileUrl)
                    .s3Key(s3Key)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            FileEntity saved = fileRepository.save(fileEntity);
            System.out.println("Saved to database: " + saved.getId());
            return saved;

        } catch (Exception e) {
            System.err.println("Upload failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public List<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }

    public void deleteFile(UUID id) {
        FileEntity fileEntity = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));

        // Xóa file trên S3
        s3Service.deleteFile(fileEntity.getS3Key());

        // Xóa thông tin file trong database
        fileRepository.delete(fileEntity);
    }
}