package com.odc.companyservice.service;

import com.odc.checklist.v1.ChecklistServiceGrpc;
import com.odc.checklist.v1.GetChecklistItemsByTemplateTypeAndEntityIdRequest;
import com.odc.common.constant.Constants;
import com.odc.common.constant.Status;
import com.odc.common.constant.Template;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.dto.SortRequest;
import com.odc.common.exception.BusinessException;
import com.odc.common.specification.GenericSpecification;
import com.odc.common.util.EnumUtil;
import com.odc.company.v1.CompanyApprovedEvent;
import com.odc.company.v1.CompanyUpdateRequestEmailEvent;
import com.odc.company.v1.ContactUser;
import com.odc.company.v1.ReviewCompanyInfoEvent;
import com.odc.companyservice.dto.request.*;
import com.odc.companyservice.dto.response.CompanyResponse;
import com.odc.companyservice.dto.response.GetCompanyByIdResponse;
import com.odc.companyservice.dto.response.GetCompanyChecklistResponse;
import com.odc.companyservice.dto.response.GetCompanyEditResponse;
import com.odc.companyservice.entity.Company;
import com.odc.companyservice.entity.CompanyDocument;
import com.odc.companyservice.event.producer.CompanyProducer;
import com.odc.companyservice.repository.CompanyRepository;
import com.odc.notification.v1.*;
import com.odc.userservice.v1.CheckEmailRequest;
import com.odc.userservice.v1.UserServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyProducer companyProducer;
    private final StringRedisTemplate stringRedisTemplate;

    @Qualifier("userServiceChannel")
    private final ManagedChannel userServiceChannel;

    @Qualifier("checklistServiceChannel")
    private final ManagedChannel checklistServiceChannel;

    public CompanyServiceImpl(CompanyRepository companyRepository,
                              CompanyProducer companyProducer,
                              StringRedisTemplate stringRedisTemplate,
                              ManagedChannel userServiceChannel,
                              ManagedChannel checklistServiceChannel) {
        this.companyRepository = companyRepository;
        this.companyProducer = companyProducer;
        this.stringRedisTemplate = stringRedisTemplate;
        this.userServiceChannel = userServiceChannel;
        this.checklistServiceChannel = checklistServiceChannel;
    }

    @Override
    public ApiResponse<CompanyResponse> registerCompany(CompanyRegisterRequest request) {
        // 1. Kiểm tra nghiệp vụ, dùng Exception tùy chỉnh
        companyRepository.findByEmail(request.getEmail()).ifPresent(c -> {
            throw new BusinessException("Email công ty đã tồn tại");
        });
        companyRepository.findByTaxCode(request.getTaxCode()).ifPresent(c -> {
            throw new BusinessException("Mã số thuế đã tồn tại");
        });
        companyRepository.findByPhone(request.getPhone()).ifPresent(c -> {
            throw new BusinessException("Số điện thoại đã tồn tại");
        });

        if (UserServiceGrpc
                .newBlockingStub(userServiceChannel)
                .checkEmailExists(CheckEmailRequest.newBuilder()
                        .setEmail(request.getContactPersonEmail())
                        .build())
                .getResult()) {
            throw new BusinessException("Email người liên hệ đã tồn tại");
        }

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
                .status(Status.PENDING_VERIFICATION.toString())
                .build();

        List<CompanyDocument> companyDocuments = new ArrayList<>();
        if (!request.getBusinessLicenseLink().isEmpty()) {
            companyDocuments.add(
                    CompanyDocument.builder()
                            .type(Constants.BUSINESS_LICENSE)
                            .fileName(request.getBusinessLicenseFileName())
                            .fileUrl(request.getBusinessLicenseLink())
                            .uploadedAt(LocalDateTime.now())
                            .company(company)
                            .build()
            );
        }

        company.setDocuments(companyDocuments);

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
    public ApiResponse<GetCompanyChecklistResponse> getCompanyChecklistByCompanyId(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        GetCompanyChecklistResponse data = GetCompanyChecklistResponse.builder().build();

        data.setChecklists(
                ChecklistServiceGrpc
                        .newBlockingStub(checklistServiceChannel)
                        .getChecklistItemsByTemplateTypeAndEntityId(GetChecklistItemsByTemplateTypeAndEntityIdRequest
                                .newBuilder()
                                .setEntityId(company.getId().toString())
                                .setTemplateType(Template.COMPANY_REGISTRATION.toString())
                                .build()).getTemplateItemsList()
                        .stream()
                        .map(templateItem -> GetCompanyChecklistResponse.GetChecklistResponse
                                .builder()
                                .id(UUID.fromString(templateItem.getId()))
                                .isChecked(templateItem.getIsChecked())
                                .build())
                        .toList()
        );

        return ApiResponse.success(data);
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
    public ApiResponse<GetCompanyByIdResponse> getCompanyById(UUID id) {
        // 1. Tìm công ty theo ID, nếu không thấy thì ném lỗi ResourceNotFoundException
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        // 2. Nếu tìm thấy, ánh xạ sang DTO và đóng gói trong ApiResponse
        return ApiResponse.<GetCompanyByIdResponse>builder()
                .success(true)
                .message("Lấy thông tin công ty thành công!")
                .timestamp(LocalDateTime.now())
                .data(mapToGetCompanyIdResponse(company)) // Tái sử dụng hàm map
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
    public void reviewCompanyInfo(ReviewCompanyInfoRequest request) {
        Company company = companyRepository.findById(UUID.fromString(request.getCreateChecklistRequest().getCompanyId()))
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + request.getCreateChecklistRequest().getCompanyId()));

        if (!EnumUtil.isEnumValueExist(request.getStatus(), Status.class)) {
            throw new RuntimeException("Trạng thái không hợp lệ.");
        }

        company.setStatus(request.getStatus());
        companyRepository.save(company);

        CreateChecklistRequest createChecklistRequest = request.getCreateChecklistRequest();

        if (company.getStatus().equalsIgnoreCase(Status.ACTIVE.toString())) {
            CompanyApprovedEvent companyApprovedEvent = CompanyApprovedEvent.newBuilder()
                    .setCompanyId(company.getId().toString())
                    .setCompanyName(company.getName())
                    .setApprovedBy(company.getUpdatedBy())
                    .setEmail(company.getEmail())
                    .setContactUser(ContactUser.newBuilder()
                            .setName(company.getContactPersonName())
                            .setEmail(company.getContactPersonEmail())
                            .setPhone(company.getContactPersonPhone())
                            .build())
                    .build();
            companyProducer.publishCompanyApprovedEmail(companyApprovedEvent);
        } else if (company.getStatus().equalsIgnoreCase(Status.UPDATE_REQUIRED.toString())) {
            List<String> incompleteChecklists = createChecklistRequest
                    .getItems()
                    .stream()
                    .map(CreateChecklistItemRequest::getTemplateItemId)
                    .toList();

            CompanyUpdateRequestEmailEvent companyUpdateRequestEmailEvent = CompanyUpdateRequestEmailEvent.newBuilder()
                    .setCompanyId(company.getId().toString())
                    .setCompanyName(company.getName())
                    .setNotes(createChecklistRequest.getNotes())
                    .setEmail(company.getEmail())
                    .addAllIncompleteChecklists(incompleteChecklists)
                    .build();

            companyProducer.publishCompanyUpdateRequestEmail(companyUpdateRequestEmailEvent);
        }

        companyProducer.sendReviewCompanyInfoEvent(ReviewCompanyInfoEvent
                .newBuilder()
                .setCreateChecklistRequest(
                        com.odc.company.v1.CreateChecklistRequest
                                .newBuilder()
                                .setTemplateId(createChecklistRequest.getTemplateId())
                                .setCompanyId(createChecklistRequest.getCompanyId())
                                .setAssigneeId(createChecklistRequest.getAssigneeId())
                                .setStatus(createChecklistRequest.getStatus())
                                .setNotes(createChecklistRequest.getNotes())
                                .addAllItems(createChecklistRequest
                                        .getItems()
                                        .stream()
                                        .map(checklist -> com.odc.company.v1.CreateChecklistItemRequest
                                                .newBuilder()
                                                .setTemplateItemId(checklist.getTemplateItemId())
                                                .setCompletedById(checklist.getCompletedById())
                                                .setIsChecked(checklist.getIsChecked())
                                                .build()
                                        )
                                        .toList())
                                .build()
                )
                .build());
    }

    @Override
    public ApiResponse<GetCompanyEditResponse> getCompanyEditByUpdateToken(String token) {
        String key = Constants.COMPANY_UPDATE_TOKEN_KEY_PREFIX + token;
        String rawId = stringRedisTemplate.opsForValue()
                .get(key);

        if (rawId == null || rawId.isEmpty()) {
            throw new BusinessException("Token không hợp lệ.");
        }

        UUID id = UUID.fromString(rawId);

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        GetCompanyEditResponse response = GetCompanyEditResponse.builder()
                .id(company.getId())
                .companyName(company.getName())
                .companyEmail(company.getEmail())
                .companyPhone(company.getPhone())
                .taxCode(company.getTaxCode())
                .address(company.getAddress())
                .contactPersonPhone(company.getContactPersonPhone())
                .contactPersonEmail(company.getContactPersonEmail())
                .contactPersonName(company.getContactPersonName())
                .getCompanyDocumentEditResponses(
                        company.getDocuments() != null ?
                                company.getDocuments()
                                        .stream()
                                        .map(documentCompany -> GetCompanyEditResponse.GetCompanyDocumentEditResponse
                                                .builder()
                                                .id(documentCompany.getId())
                                                .fileUrl(documentCompany.getFileUrl())
                                                .type(documentCompany.getType())
                                                .fileName(documentCompany.getFileName())
                                                .build())
                                        .toList()
                                : new ArrayList<>()
                )
                .build();

        return ApiResponse.success(response);
    }

    @Override
    public ApiResponse<Void> updateCompanyOnboard(String token, UpdateCompanyRegistrationRequest request) {
        String key = Constants.COMPANY_UPDATE_TOKEN_KEY_PREFIX + token;
        String rawId = stringRedisTemplate.opsForValue().get(key);

        if (rawId == null || rawId.isEmpty()) {
            throw new BusinessException("Token không hợp lệ.");
        }

        UUID id = UUID.fromString(rawId);

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty với ID: " + id));

        if (!company.getTaxCode().equalsIgnoreCase(request.getTaxCode()))
            companyRepository.findByTaxCode(request.getTaxCode()).ifPresent(c -> {
                throw new BusinessException("Mã số thuế đã tồn tại");
            });

        if (!company.getPhone().equalsIgnoreCase(request.getPhone()))
            companyRepository.findByPhone(request.getPhone()).ifPresent(c -> {
                throw new BusinessException("Số điện thoại đã tồn tại");
            });

        if (!company.getContactPersonEmail().equalsIgnoreCase(request.getContactPersonEmail()) && UserServiceGrpc
                .newBlockingStub(userServiceChannel)
                .checkEmailExists(CheckEmailRequest.newBuilder()
                        .setEmail(request.getContactPersonEmail())
                        .build())
                .getResult()) {
            throw new BusinessException("Email người liên hệ đã tồn tại");
        }

        company.setName(request.getName());
        if (!company.getPhone().equalsIgnoreCase(request.getPhone()))
            company.setPhone(request.getPhone());

        if (!company.getTaxCode().equalsIgnoreCase(request.getTaxCode()))
            company.setTaxCode(request.getTaxCode());

        company.setAddress(request.getAddress());
        company.setContactPersonEmail(request.getContactPersonEmail());
        company.setContactPersonName(request.getContactPersonName());
        company.setContactPersonPhone(request.getContactPersonPhone());
        company.setStatus(Status.PENDING.toString());

        if (company.getDocuments() == null) {
            company.setDocuments(new ArrayList<>());
        }

        List<UpdateCompanyDocumentRequest> docRequests = request.getUpdateCompanyDocumentRequests();
        if (docRequests != null && !docRequests.isEmpty()) {

            Map<UUID, CompanyDocument> existingMap = company.getDocuments().stream()
                    .filter(doc -> doc.getId() != null)
                    .collect(Collectors.toMap(CompanyDocument::getId, doc -> doc));

            List<CompanyDocument> updatedList = new ArrayList<>();

            for (UpdateCompanyDocumentRequest req : docRequests) {
                if (req.getId() != null && existingMap.containsKey(req.getId())) {
                    CompanyDocument existing = existingMap.get(req.getId());
                    existing.setFileUrl(req.getFileUrl());
                    existing.setType(req.getType());
                    existing.setFileName(req.getFileName());
                    updatedList.add(existing);
                } else {
                    CompanyDocument newDoc = CompanyDocument.builder()
                            .fileUrl(req.getFileUrl())
                            .type(req.getType())
                            .fileName(req.getFileName())
                            .company(company)
                            .build();
                    updatedList.add(newDoc);
                }
            }

            company.getDocuments().removeIf(doc ->
                    doc.getId() != null &&
                            updatedList.stream().noneMatch(u -> u.getId() != null && u.getId().equals(doc.getId()))
            );

            company.getDocuments().clear();
            company.getDocuments().addAll(updatedList);
        }

        companyRepository.save(company);
        stringRedisTemplate.delete(key);

        Map<String, String> dataMap = Map.of(
                "companyId", company.getId().toString(),
                "companyName", company.getName()
        );

        RoleTarget roleTarget = RoleTarget.newBuilder()
                .addRoles("LAB_ADMIN")
                .build();

        Target target = Target.newBuilder()
                .setRole(roleTarget)
                .build();

        NotificationEvent notificationEvent = NotificationEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setType("COMPANY_UPDATE")
                .setTitle("Company Information Updated")
                .setContent("The company \"" + company.getName() + "\" has updated its registration details and is awaiting verification.")
                .putAllData(dataMap)
                .setDeepLink("/approve?id=" + company.getId())
                .setPriority("HIGH")
                .setTarget(target)
                .addAllChannels(List.of(Channel.WEB))
                .setCreatedAt(System.currentTimeMillis())
                .setCategory("COMPANY_MANAGEMENT")
                .build();
        companyProducer.publishNotificationCompanyUpdateEvent(notificationEvent);

        return ApiResponse.success("Cập nhật thông tin công ty thành công.", null);
    }

    @Override
    public ApiResponse<List<CompanyResponse>> searchCompanies(SearchRequest request) {
        Specification<Company> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);

        List<CompanyResponse> companies = companyRepository.findAll(specification, sort)
                .stream()
                .map(this::mapToSearchResponse) // Sử dụng mapper riêng không có documents
                .collect(Collectors.toList());

        return ApiResponse.<List<CompanyResponse>>builder()
                .success(true)
                .message("Tìm kiếm công ty thành công")
                .timestamp(LocalDateTime.now())
                .data(companies)
                .build();
    }

    @Override
    public ApiResponse<PaginatedResult<CompanyResponse>> searchCompaniesWithPagination(SearchRequest request) {
        Specification<Company> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        Page<CompanyResponse> page = companyRepository.findAll(specification, pageable)
                .map(this::mapToSearchResponse); // Sử dụng mapper riêng không có documents

        return ApiResponse.success(PaginatedResult.from(page));
    }

    @Override
    public ApiResponse<CompanyResponse> getCompanyByUserId(UUID userId) {
        Company company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy công ty cho người dùng với ID: " + userId));

        CompanyResponse responseData = mapToResponse(company);
        return ApiResponse.success("Lấy thông tin công ty thành công", responseData);
    }

    private CompanyResponse mapToSearchResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .email(company.getEmail())
                .phone(company.getPhone())
                .taxCode(company.getTaxCode())
                .address(company.getAddress())
                .description(company.getDescription())
                .website(company.getWebsite())
                .status(company.getStatus())
                .domain(company.getDomain())
                .contactPersonPhone(company.getContactPersonPhone())
                .contactPersonEmail(company.getContactPersonEmail())
                .contactPersonName(company.getContactPersonName())
                .createdAt(company.getCreatedAt())
                .getCompanyDocumentResponses(null)
                .build();
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
                .description(company.getDescription())
                .website(company.getWebsite())
                .status(company.getStatus())
                .domain(company.getDomain())
                .contactPersonPhone(company.getContactPersonPhone())
                .contactPersonEmail(company.getContactPersonEmail())
                .contactPersonName(company.getContactPersonName())
                .createdAt(company.getCreatedAt())
                .getCompanyDocumentResponses(
                        company.getDocuments() == null ? new ArrayList<>()
                                : company.getDocuments()
                                .stream()
                                .filter(document -> Constants.BUSINESS_LICENSE.equalsIgnoreCase(document.getType()))
                                .map(document -> CompanyResponse
                                        .GetCompanyDocumentResponse.builder()
                                        .id(document.getId())
                                        .fileUrl(document.getFileUrl())
                                        .fileName(document.getFileName())
                                        .type(document.getType())
                                        .build())
                                .toList()
                )
                .build();
    }

    private GetCompanyByIdResponse mapToGetCompanyIdResponse(Company company) {
        return GetCompanyByIdResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .email(company.getEmail())
                .phone(company.getPhone())
                .taxCode(company.getTaxCode())
                .address(company.getAddress())
                .description(company.getDescription())
                .website(company.getWebsite())
                .status(company.getStatus())
                .domain(company.getDomain())
                .userId(company.getUserId() == null ? "" : company.getUserId().toString())
                .contactPersonPhone(company.getContactPersonPhone())
                .contactPersonEmail(company.getContactPersonEmail())
                .contactPersonName(company.getContactPersonName())
                .createdAt(company.getCreatedAt())
                .getCompanyDocumentResponses(
                        company.getDocuments() == null ? new ArrayList<>()
                                : company.getDocuments()
                                .stream()
                                .filter(document -> Constants.BUSINESS_LICENSE.equalsIgnoreCase(document.getType()))
                                .map(document -> CompanyResponse
                                        .GetCompanyDocumentResponse.builder()
                                        .id(document.getId())
                                        .fileUrl(document.getFileUrl())
                                        .type(document.getType())
                                        .build())
                                .toList()
                )
                .build();
    }
}
