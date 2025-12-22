package com.odc.companyservice.grpc;

import com.odc.companyservice.entity.Company;
import com.odc.companyservice.repository.CompanyRepository;
import com.odc.companyservice.v1.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;
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
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty cho id: " + request.getCompanyId()));

            GetCompanyByIdResponse response = GetCompanyByIdResponse
                    .newBuilder()
                    .setCompanyName(company.getName())
                    .setContactPersonEmail(company.getContactPersonEmail())
                    .setUserId(
                            Optional.ofNullable(company.getUserId())
                                    .map(Object::toString)
                                    .orElse("")
                    )
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.info("error: {}", e.getMessage());
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCompaniesByIds(GetCompaniesByIdsRequest request, StreamObserver<GetCompaniesByIdsResponse> responseObserver) {
        try {
            var companyIds = request.getCompanyIdsList();

            log.info("Received request to get companies by IDs: {}", companyIds);

            var uuidList = companyIds.stream()
                    .map(UUID::fromString)
                    .toList();

            var companies = companyRepository.findAllById(uuidList);

            GetCompaniesByIdsResponse.Builder responseBuilder = GetCompaniesByIdsResponse.newBuilder();

            companies.forEach(company ->
                    responseBuilder.putCompanyNames(
                            company.getId().toString(),
                            company.getName()
                    )
            );

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getCompaniesByIds: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCompaniesInfoByIds(GetInfoCompanyByCompanyIdsRequest request, StreamObserver<GetInfoCompanyByCompanyIdsResponse> responseObserver) {
        try {
            var companyIds = request.getIdsList();

            log.info("Received request to get companies by IDs: {}", companyIds);

            var uuidList = companyIds.stream()
                    .map(UUID::fromString)
                    .toList();

            var companies = companyRepository.findAllById(uuidList);

            GetInfoCompanyByCompanyIdsResponse.Builder responseBuilder = GetInfoCompanyByCompanyIdsResponse.newBuilder();

            companies.forEach(company ->
                    responseBuilder.putData(company.getId().toString(),
                            CompanyInfo.newBuilder()
                                    .setCompanyName(company.getName())
                                    .setCompanyEmail(company.getEmail())
                                    .setCompanyLogo(company.getLogo())
                                    .setUserId(company.getUserId().toString())
                                    .build()
                    )
            );

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in getCompaniesByIds: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}
