package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.dto.SortRequest;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.common.specification.GenericSpecification;
import com.odc.projectservice.dto.request.CreateSkillRequest;
import com.odc.projectservice.dto.request.UpdateSkillRequest;
import com.odc.projectservice.dto.response.SkillResponse;
import com.odc.projectservice.entity.Skill;
import com.odc.projectservice.repository.SkillRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SkillServiceImpl implements SkillService {
    private final SkillRepository skillRepository;

    @Override
    public ApiResponse<SkillResponse> createSkill(CreateSkillRequest request) {
        if (skillRepository.existsByName(request.getName())) {
            return ApiResponse.error("Kỹ năng với tên '" + request.getName() + "' đã tồn tại");
        }
        Skill skill = Skill.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Skill savedSkill = skillRepository.save(skill);
        SkillResponse skillResponse = SkillResponse.builder()
                .id(savedSkill.getId())
                .name(savedSkill.getName())
                .description(savedSkill.getDescription())
                .build();

        return ApiResponse.success("Tạo kỹ năng thành công", skillResponse);
    }

    @Override
    public ApiResponse<SkillResponse> updateSkill(UUID id, UpdateSkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kỹ năng không tồn tại: " + id));

        if (!skill.getName().equals(request.getName()) && skillRepository.existsByName(request.getName())) {
            return ApiResponse.error("Kỹ năng với tên '" + request.getName() + "' đã tồn tại");
        }

        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setIsDeleted(request.getIsDeleted());
        Skill updatedSkill = skillRepository.save(skill);
        SkillResponse skillResponse = SkillResponse.builder()
                .id(updatedSkill.getId())
                .name(updatedSkill.getName())
                .description(updatedSkill.getDescription())
                .isDeleted(!updatedSkill.getIsDeleted())
                .build();
        return ApiResponse.success("Cập nhật kỹ năng thành công", skillResponse);
    }

    @Override
    public ApiResponse<SkillResponse> deleteSkill(UUID id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kỹ năng không tồn tại: " + id));

        if (!skill.getProjects().isEmpty()) {
            return ApiResponse.error("Không thể xóa kỹ năng '" + skill.getName() + "' vì đang được sử dụng trong " + skill.getProjects().size() + " dự án");
        }

        skillRepository.delete(skill);
        return ApiResponse.success("Xóa kỹ năng thành công", null);
    }

    @Override
    public ApiResponse<List<SkillResponse>> getAllSkills() {
        List<Skill> skills = skillRepository.findAll();
        List<SkillResponse> skillResponses = skills.stream()
                .map(this::convertToSkillResponse)
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách kỹ năng thành công", skillResponses);
    }

    @Override
    public ApiResponse<SkillResponse> getSkillById(UUID id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kỹ năng không tồn tại: " + id));

        SkillResponse skillResponse = convertToSkillResponse(skill);
        return ApiResponse.success("Lấy thông tin kỹ năng thành công", skillResponse);
    }

    @Override
    public ApiResponse<List<SkillResponse>> searchSkills(SearchRequest request) {
        Specification<Skill> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);
        List<SkillResponse> skills = skillRepository.findAll(specification, sort)
                .stream()
                .map(this::convertToSkillResponse)
                .collect(Collectors.toList());

        return ApiResponse.<List<SkillResponse>>builder()
                .success(true)
                .message("Tìm kiếm kỹ năng thành công")
                .timestamp(LocalDateTime.now())
                .data(skills)
                .build();
    }

    @Override
    public ApiResponse<PaginatedResult<SkillResponse>> searchSkillsWithPagination(SearchRequest request) {
        Specification<Skill> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);
        Page<SkillResponse> page = skillRepository.findAll(specification, pageable).map(this::convertToSkillResponse);

        return ApiResponse.success(PaginatedResult.from(page));
    }

    private SkillResponse convertToSkillResponse(Skill skill) {
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .isDeleted(skill.getIsDeleted())
                .build();
    }
}
