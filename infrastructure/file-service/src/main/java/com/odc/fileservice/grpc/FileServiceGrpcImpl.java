package com.odc.fileservice.grpc;

import com.odc.fileservice.entity.FileEntity;
import com.odc.fileservice.repository.FileRepository;
import com.odc.fileservice.v1.FileServiceGrpc;
import com.odc.fileservice.v1.GetFileNamesByLinksRequest;
import com.odc.fileservice.v1.GetFileNamesByLinksResponse;
import com.odc.fileservice.v1.GetFilesByEntityIdRequest;
import com.odc.fileservice.v1.GetFilesByEntityIdResponse;
import com.odc.fileservice.v1.FileInfo;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class FileServiceGrpcImpl extends FileServiceGrpc.FileServiceImplBase {

    private final FileRepository fileRepository;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void getFileNamesByLinks(GetFileNamesByLinksRequest request,
                                    StreamObserver<GetFileNamesByLinksResponse> responseObserver) {
        try {
            List<String> fileLinks = request.getFileLinksList();

            if (fileLinks == null || fileLinks.isEmpty()) {
                responseObserver.onNext(GetFileNamesByLinksResponse.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }

            List<FileEntity> files = fileRepository.findByFileUrlIn(fileLinks);

            GetFileNamesByLinksResponse.Builder responseBuilder =
                    GetFileNamesByLinksResponse.newBuilder();

            Map<String, String> linkToName = responseBuilder.getFileLinkToNameMap();

            files.forEach(f ->
                    responseBuilder.putFileLinkToName(f.getFileUrl(), f.getFileName())
            );

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception ex) {
            log.error("Error in getFileNamesByLinks: ", ex);
            responseObserver.onError(ex);
        }
    }

    @Override
    public void getFilesByEntityId(GetFilesByEntityIdRequest request,
                                   StreamObserver<GetFilesByEntityIdResponse> responseObserver) {
        try {
            String entityId = request.getEntityId();

            if (entityId == null || entityId.isEmpty()) {
                responseObserver.onNext(GetFilesByEntityIdResponse.newBuilder().build());
                responseObserver.onCompleted();
                return;
            }

            List<FileEntity> files = fileRepository.findByEntityId(entityId);

            GetFilesByEntityIdResponse.Builder responseBuilder =
                    GetFilesByEntityIdResponse.newBuilder();

            files.forEach(file -> {
                FileInfo fileInfo = FileInfo.newBuilder()
                        .setId(file.getId().toString())
                        .setFileName(file.getFileName())
                        .setFileUrl(file.getFileUrl())
                        .setS3Key(file.getS3Key())
                        .setUploadedAt(file.getUploadedAt().format(DATE_TIME_FORMATTER))
                        .setEntityId(file.getEntityId() != null ? file.getEntityId() : "")
                        .build();
                responseBuilder.addFiles(fileInfo);
            });

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception ex) {
            log.error("Error in getFilesByEntityId: ", ex);
            responseObserver.onError(ex);
        }
    }
}