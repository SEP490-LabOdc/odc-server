package com.odc.companyservice.grpc;

import com.odc.companyservice.entity.Company;
import com.odc.companyservice.repository.CompanyRepository;
import com.odc.companyservice.v1.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class CompanyServiceGrpcImpl extends CompanyServiceGrpc.CompanyServiceImplBase {
    private final CompanyRepository companyRepository;

    @Override
    public void getCompanyByUserId(GetCompanyByUserIdRequest request, StreamObserver<GetCompanyByUserIdResponse> responseObserver) {
        try {
            String userId = request.getUserId();

            Company company = companyRepository.findByUserId(UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty cho userId: " + userId));

            GetCompanyByUserIdResponse response = GetCompanyByUserIdResponse.newBuilder()
                    .setCompanyId(company.getId().toString())
                    .setCompanyName(company.getName())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("error: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCompanyById(GetCompanyByIdRequest request, StreamObserver<GetCompanyByIdResponse> responseObserver) {
        try {
            Company company = companyRepository.findById(UUID.fromString(request.getCompanyId()))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty cho id: " +  request.getCompanyId()));

            GetCompanyByIdResponse response = GetCompanyByIdResponse
                    .newBuilder()
                    .setCompanyName(company.getName())
                    .setContactPersonEmail(company.getContactPersonEmail())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.info("error: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }
}
