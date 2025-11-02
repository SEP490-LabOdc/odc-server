package com.odc.companyservice.service;

import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.companyservice.dto.request.CompanyRegisterRequest;
import com.odc.companyservice.dto.request.ReviewCompanyInfoRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRegistrationRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRequest;
import com.odc.companyservice.dto.response.CompanyResponse;
import com.odc.companyservice.dto.response.GetCompanyChecklistResponse;
import com.odc.companyservice.dto.response.GetCompanyEditResponse;

import java.util.List;
import java.util.UUID;

public interface CompanyService {
    ApiResponse<CompanyResponse> registerCompany(CompanyRegisterRequest request);

    ApiResponse<CompanyResponse> updateCompany(UUID id, UpdateCompanyRequest request);

    ApiResponse<List<CompanyResponse>> getAllCompanies();

    ApiResponse<CompanyResponse> getCompanyById(UUID id);

    ApiResponse<Void> deleteCompany(UUID id);

    void updateRegisterCompanyStatus(UUID id, Status status);

    void reviewCompanyInfo(ReviewCompanyInfoRequest request);

    ApiResponse<GetCompanyChecklistResponse> getCompanyChecklistByCompanyId(UUID id);

    ApiResponse<GetCompanyEditResponse> getCompanyEditByUpdateToken(String token);

    ApiResponse<Void> updateCompanyOnboard(String token, UpdateCompanyRegistrationRequest request);

    ApiResponse<List<CompanyResponse>> searchCompanies(SearchRequest request);

    ApiResponse<PaginatedResult<CompanyResponse>> searchCompaniesWithPagination(SearchRequest request);
}
