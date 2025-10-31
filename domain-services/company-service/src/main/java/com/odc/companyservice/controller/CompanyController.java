package com.odc.companyservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.companyservice.dto.request.CompanyRegisterRequest;
import com.odc.companyservice.dto.request.ReviewCompanyInfoRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRegistrationRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRequest;
import com.odc.companyservice.dto.response.CompanyResponse;
import com.odc.companyservice.dto.response.GetCompanyChecklistResponse;
import com.odc.companyservice.dto.response.GetCompanyEditResponse;
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
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(@PathVariable UUID id) {
        ApiResponse<CompanyResponse> response = companyService.getCompanyById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
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
}
