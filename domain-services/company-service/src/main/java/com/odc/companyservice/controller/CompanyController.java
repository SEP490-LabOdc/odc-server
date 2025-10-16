package com.odc.companyservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.companyservice.dto.request.CompanyRegisterRequest;
import com.odc.companyservice.dto.request.ReviewCompanyInfoRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRequest;
import com.odc.companyservice.dto.response.CompanyResponse;
import com.odc.companyservice.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CompanyResponse>> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return new ResponseEntity<>(companyService.registerCompany(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        ApiResponse<CompanyResponse> response = companyService.updateCompany(id, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateCompanyRegisterStatus(@PathVariable UUID id, @Valid @RequestBody ReviewCompanyInfoRequest request) {
        companyService.reviewCompanyInfo(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đăng ký thành công.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<CompanyResponse>>> getAllCompanies() {
        ApiResponse<java.util.List<CompanyResponse>> response = companyService.getAllCompanies();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(@PathVariable UUID id) {
        ApiResponse<CompanyResponse> response = companyService.getCompanyById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable UUID id) {
        ApiResponse<Void> response = companyService.deleteCompany(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
