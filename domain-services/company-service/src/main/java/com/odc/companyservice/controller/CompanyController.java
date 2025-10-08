package com.odc.companyservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.companyservice.dto.request.CompanyRegisterRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRequest;
import com.odc.companyservice.dto.response.CompanyResponse;
import com.odc.companyservice.service.CompanyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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

    @SecurityRequirements({})
    @PostMapping("/register")
    public ResponseEntity<CompanyResponse> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        CompanyResponse responseDTO = companyService.registerCompany(request).getData();
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        ApiResponse<CompanyResponse> response = companyService.updateCompany(id, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
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
