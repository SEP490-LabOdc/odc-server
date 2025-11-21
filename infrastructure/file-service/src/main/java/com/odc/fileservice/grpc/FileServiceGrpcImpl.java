package com.odc.fileservice.grpc;

import com.odc.fileservice.entity.FileEntity;
import com.odc.fileservice.repository.FileRepository;
import com.odc.fileservice.v1.FileServiceGrpc;
import com.odc.fileservice.v1.GetFileNamesByLinksRequest;
import com.odc.fileservice.v1.GetFileNamesByLinksResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Map;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class FileServiceGrpcImpl extends FileServiceGrpc.FileServiceImplBase {

    private final FileRepository fileRepository;

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
}

