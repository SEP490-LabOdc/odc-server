package com.odc.companyservice.service;

import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.company.v1.ReviewCompanyInfoEvent;
import com.odc.companyservice.dto.request.CompanyRegisterRequest;
import com.odc.companyservice.dto.request.ReviewCompanyInfoRequest;
import com.odc.companyservice.dto.request.UpdateCompanyRequest;
import com.odc.companyservice.dto.response.CompanyResponse;
import com.odc.companyservice.entity.Company;
import com.odc.companyservice.event.producer.CompanyProducer;
import com.odc.companyservice.repository.CompanyRepository;
import com.odc.notification.v1.SendOtpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyProducer companyProducer;

    @Override
    @Transactional
    public ApiResponse<CompanyResponse> registerCompany(CompanyRegisterRequest request) {
        // 1. Kiểm tra nghiệp vụ, dùng Exception tùy chỉnh
        companyRepository.findByEmail(request.getEmail()).ifPresent(c -> {
            throw new BusinessException("Email công ty đã tồn tại");
        });
        companyRepository.findByTaxCode(request.getTaxCode()).ifPresent(c -> {
            throw new BusinessException("Mã số thuế đã tồn tại");
        });

        // 2. Ánh xạ từ DTO sang Entity
        Company company = Company.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .taxCode(request.getTaxCode())
                .address(request.getAddress())
                .description("")
                .website("")
                .domain("")
                .logo("")
                .contactPersonEmail(request.getContactPersonEmail())
                .contactPersonName(request.getContactPersonName())
                .contactPersonPhone(request.getContactPersonPhone())
                .status(Status.PENDING.toString())
                .build();

        // 3. Lưu vào database
        Company savedCompany = companyRepository.save(company);

        // 4. Ánh xạ từ Entity sang Response DTO
        CompanyResponse responseData = mapToResponse(savedCompany);

        companyProducer.sendOtpEmailEvent(SendOtpRequest
                .newBuilder()
                .setEmail(company.getEmail())
                .build());

        // 5. Trả về trong cấu trúc ApiResponse chuẩn
        return ApiResponse.<CompanyResponse>builder()
                .success(true)
                .message("Đăng ký công ty thành công!")
                .timestamp(LocalDateTime.now())
                .data(responseData)
                .build();
    }

    @Override
    public ApiResponse<CompanyResponse> updateCompany(UUID id, UpdateCompanyRequest request) {
        // 1. Tìm công ty theo ID, nếu không thấy thì ném lỗi
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        // 2. Cập nhật các trường thông tin từ request
        company.setName(request.getName());
        company.setPhone(request.getPhone());
        company.setAddress(request.getAddress());
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setLogo(request.getLogo());
        company.setBanner(request.getBanner());
        company.setDomain(request.getDomain());

        // 3. Lưu lại vào database
        Company updatedCompany = companyRepository.save(company);

        // 4. Ánh xạ sang DTO và đóng gói trong ApiResponse
        return ApiResponse.<CompanyResponse>builder()
                .success(true)
                .message("Cập nhật thông tin công ty thành công!")
                .timestamp(LocalDateTime.now())
                .data(mapToResponse(updatedCompany)) // Sử dụng hàm map dùng chung
                .build();
    }

    @Override
    public ApiResponse<List<CompanyResponse>> getAllCompanies() {
        // 1. Lấy tất cả Company entity từ database
        List<Company> companies = companyRepository.findAll();

        // 2. Dùng stream để chuyển đổi List<Company> thành List<CompanyResponseDTO>
        List<CompanyResponse> responseData = companies.stream()
                .map(this::mapToResponse) // Tái sử dụng hàm map đã có
                .collect(Collectors.toList());

        // 3. Đóng gói danh sách vào trong ApiResponse
        return ApiResponse.<List<CompanyResponse>>builder()
                .success(true)
                .message("Lấy danh sách công ty thành công!")
                .timestamp(LocalDateTime.now())
                .data(responseData)
                .build();
    }

    @Override
    public ApiResponse<CompanyResponse> getCompanyById(UUID id) {
        // 1. Tìm công ty theo ID, nếu không thấy thì ném lỗi ResourceNotFoundException
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        // 2. Nếu tìm thấy, ánh xạ sang DTO và đóng gói trong ApiResponse
        return ApiResponse.<CompanyResponse>builder()
                .success(true)
                .message("Lấy thông tin công ty thành công!")
                .timestamp(LocalDateTime.now())
                .data(mapToResponse(company)) // Tái sử dụng hàm map
                .build();
    }

    @Override
    public ApiResponse<Void> deleteCompany(UUID id) {
        // 1. Kiểm tra công ty có tồn tại không
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        // 2. Xóa công ty
        companyRepository.delete(company);

        // 3. Trả về ApiResponse thành công, không có data
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa công ty thành công!")
                .timestamp(LocalDateTime.now())
                .data(null)
                .build();
    }

    @Override
    public void updateRegisterCompanyStatus(UUID id, Status status) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        company.setStatus(status.toString());
        companyRepository.save(company);
    }

    @Override
    public void reviewCompanyInfo(UUID id, ReviewCompanyInfoRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        company.setStatus(request.getStatus());
        companyRepository.save(company);

        companyProducer.sendReviewCompanyInfoEvent(ReviewCompanyInfoEvent
                .newBuilder()
                .setCreateChecklistRequest(request.getCreateChecklistRequest())
                .build());
    }

    // --- Private Helper Method để tránh lặp code ---
    private CompanyResponse mapToResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .email(company.getEmail())
                .phone(company.getPhone())
                .taxCode(company.getTaxCode())
                .address(company.getAddress())
                .website(company.getWebsite())
                .status(company.getStatus())
                .domain(company.getDomain())
                .contactPersonPhone(company.getContactPersonPhone())
                .contactPersonEmail(company.getContactPersonEmail())
                .contactPersonName(company.getContactPersonName())
                .createdAt(company.getCreatedAt())
                .build();
    }

}
