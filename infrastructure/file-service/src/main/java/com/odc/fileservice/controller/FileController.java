package com.odc.fileservice.controller;

import com.odc.common.constant.ApiConstants;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.fileservice.entity.FileEntity;
import com.odc.fileservice.service.FileService;
import com.odc.fileservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final S3Service s3Service;
    private final com.odc.fileservice.repository.FileRepository fileRepository;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileEntity>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "entityId", required = false) String entityId) throws IOException {

        if (file.isEmpty()) {
            throw new BusinessException("File is empty", ApiConstants.VALIDATION_ERROR);
        }

        FileEntity uploadedFile = fileService.uploadFile(file, entityId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", uploadedFile));
    }

    @GetMapping
    public ResponseEntity<List<FileEntity>> getAllFiles() {
        return ResponseEntity.ok(fileService.getAllFiles());
    }

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<FileEntity>> getFilesByEntityId(@PathVariable String entityId) {
        List<FileEntity> files = fileService.getFilesByEntityId(entityId);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }
}

