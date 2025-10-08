package com.odc.checklistservice.service;

import com.odc.checklistservice.dto.request.CreateChecklistTemplateRequest;
import com.odc.checklistservice.dto.request.UpdateChecklistTemplateRequest;
import com.odc.checklistservice.dto.response.GetChecklistTemplateResponse;
import com.odc.checklistservice.entity.ChecklistTemplate;
import com.odc.checklistservice.entity.TemplateGroup;
import com.odc.checklistservice.entity.TemplateItem;
import com.odc.checklistservice.repository.ChecklistTemplateRepository;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChecklistTemplateServiceImpl implements ChecklistTemplateService {
    private final ChecklistTemplateRepository templateRepository;

    @Override
    public ApiResponse<UUID> createChecklistTemplate(CreateChecklistTemplateRequest request) {
        ChecklistTemplate template = mapCreateRequestToEntity(request);
        ChecklistTemplate savedTemplate = templateRepository.save(template);
        return ApiResponse.success("Tạo checklist template thành công.", savedTemplate.getId());
    }

    @Override
    @Transactional
    public ApiResponse<UUID> updateChecklistTemplate(UUID id, UpdateChecklistTemplateRequest request) {
        ChecklistTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy template với ID: " + id));

        existingTemplate.setName(request.getName());
        existingTemplate.setDescription(request.getDescription());

        existingTemplate.getGroups().clear();

        if (request.getGroups() != null) {
            for (UpdateChecklistTemplateRequest.GroupRequest groupRequest : request.getGroups()) {
                TemplateGroup newGroup = new TemplateGroup();
                newGroup.setTitle(groupRequest.getTitle());
                newGroup.setDisplayOrder(groupRequest.getDisplayOrder());
                newGroup.setTemplate(existingTemplate);

                if (groupRequest.getItems() != null) {
                    for (UpdateChecklistTemplateRequest.ItemRequest itemRequest : groupRequest.getItems()) {
                        TemplateItem newItem = new TemplateItem();
                        newItem.setContent(itemRequest.getContent());
                        newItem.setDisplayOrder(itemRequest.getDisplayOrder());
                        newItem.setIsRequired(itemRequest.isRequired());
                        newItem.setGroup(newGroup);
                        newGroup.getItems().add(newItem);
                    }
                }
                existingTemplate.getGroups().add(newGroup);
            }
        }

        ChecklistTemplate updatedTemplate = templateRepository.save(existingTemplate);
        return ApiResponse.success("Cập nhật checklist template thành công.", updatedTemplate.getId());
    }

    @Override
    @Transactional
    public ApiResponse<UUID> deleteChecklistTemplate(UUID id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy template với ID: " + id);
        }
        templateRepository.deleteById(id);
        return ApiResponse.success("Xóa checklist template thành công.", id);
    }

    @Override
    public ApiResponse<GetChecklistTemplateResponse> getChecklistTemplateById(UUID id) {
        ChecklistTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy template với ID: " + id));

        GetChecklistTemplateResponse responseDto = mapEntityToGetResponseDto(template);
        return ApiResponse.success("Lấy thông tin template thành công.", responseDto);
    }

    private ChecklistTemplate mapCreateRequestToEntity(CreateChecklistTemplateRequest request) {
        List<TemplateGroup> groups = new ArrayList<>();
        ChecklistTemplate template = new ChecklistTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setEntityType(request.getEntityType());

        if (request.getGroups() != null) {
            for (CreateChecklistTemplateRequest.GroupRequest groupRequest : request.getGroups()) {
                TemplateGroup group = new TemplateGroup();
                group.setTitle(groupRequest.getTitle());
                group.setDisplayOrder(groupRequest.getDisplayOrder());
                group.setTemplate(template);

                List<TemplateItem> items = new ArrayList<>();

                if (groupRequest.getItems() != null) {
                    for (CreateChecklistTemplateRequest.ItemRequest itemRequest : groupRequest.getItems()) {
                        TemplateItem item = new TemplateItem();
                        item.setContent(itemRequest.getContent());
                        item.setDisplayOrder(itemRequest.getDisplayOrder());
                        item.setIsRequired(itemRequest.isRequired());
                        item.setGroup(group);
                        items.add(item);
                    }
                }

                group.setItems(items);
                groups.add(group);
            }
        }
        template.setGroups(groups);
        return template;
    }

    private GetChecklistTemplateResponse mapEntityToGetResponseDto(ChecklistTemplate entity) {
        GetChecklistTemplateResponse response = new GetChecklistTemplateResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setEntityType(entity.getEntityType());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getGroups() != null) {
            response.setGroups(entity.getGroups().stream().map(group -> {
                GetChecklistTemplateResponse.GroupResponse groupResponse = new GetChecklistTemplateResponse.GroupResponse();
                groupResponse.setId(group.getId());
                groupResponse.setTitle(group.getTitle());
                groupResponse.setDisplayOrder(group.getDisplayOrder());

                if (group.getItems() != null) {
                    groupResponse.setItems(group.getItems().stream().map(item -> {
                        GetChecklistTemplateResponse.ItemResponse itemResponse = new GetChecklistTemplateResponse.ItemResponse();
                        itemResponse.setId(item.getId());
                        itemResponse.setContent(item.getContent());
                        itemResponse.setDisplayOrder(item.getDisplayOrder());
                        itemResponse.setRequired(item.getIsRequired());
                        return itemResponse;
                    }).collect(Collectors.toList()));
                }
                return groupResponse;
            }).collect(Collectors.toList()));
        }
        return response;
    }
}
