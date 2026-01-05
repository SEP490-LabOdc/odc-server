package com.odc.companyservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.SearchRequest;
import com.odc.companyservice.dto.request.CompanyRegisterRequest;
import com.odc.companyservice.dto.request.ReviewCompanyInfoRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRegistrationRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRequest;
import com.odc.companyservice.dto.response.*;
import com.odc.companyservice.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/for-update")
    public ResponseEntity<ApiResponse<GetCompanyEditResponse>> getEditCompany(@RequestParam String token) {
        return ResponseEntity.ok(companyService.getCompanyEditByUpdateToken(token));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CompanyResponse>> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return new ResponseEntity<>(companyService.registerCompany(request), HttpStatus.CREATED);
    }

    @PutMapping("/onboard/update")
    public ResponseEntity<ApiResponse<Void>> updateCompanyOnboard(@RequestParam String token,
                                                                  @Valid @RequestBody UpdateCompanyRegistrationRequest request) {
        return ResponseEntity.ok(companyService.updateCompanyOnboard(token, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        ApiResponse<CompanyResponse> response = companyService.updateCompany(id, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/review")
    public ResponseEntity<ApiResponse<Void>> updateCompanyRegisterStatus(@Valid @RequestBody ReviewCompanyInfoRequest request) {
        companyService.reviewCompanyInfo(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đăng ký thành công.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<CompanyResponse>>> getAllCompanies() {
        ApiResponse<java.util.List<CompanyResponse>> response = companyService.getAllCompanies();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetCompanyByIdResponse>> getCompanyById(@PathVariable UUID id) {
        ApiResponse<GetCompanyByIdResponse> response = companyService.getCompanyById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<PublicCompanyResponse>> getPublicCompanyDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.getPublicCompanyById(id));
    }

    @GetMapping("/{id}/checklists")
    public ResponseEntity<ApiResponse<GetCompanyChecklistResponse>> getCompanyChecklists(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.getCompanyChecklistByCompanyId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable UUID id) {
        ApiResponse<Void> response = companyService.deleteCompany(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchCompanies(@RequestBody SearchRequest request) {
        boolean isPaginated = request.getPage() != null && request.getSize() != null;

        if (isPaginated) {
            return ResponseEntity.ok(companyService.searchCompaniesWithPagination(request));
        }

        return ResponseEntity.ok(companyService.searchCompanies(request));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetMyCompanyResponse>> getMyCompany() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(companyService.getMyCompany(userId));
    }

    @GetMapping("/by-user-id/{userId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyByUserId(@PathVariable UUID userId) {
        ApiResponse<CompanyResponse> response = companyService.getCompanyByUserId(userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/statistics/new-last-6-months")
    public ResponseEntity<ApiResponse<List<CompanyMonthlyStatisticResponse>>> getNewCompaniesLast6Months() {
        return ResponseEntity.ok(
                ApiResponse.success(companyService.getNewCompaniesLast6Months())
        );
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview() {
        return ResponseEntity.ok(
                ApiResponse.success(companyService.getOverview())
        );
    }
}
