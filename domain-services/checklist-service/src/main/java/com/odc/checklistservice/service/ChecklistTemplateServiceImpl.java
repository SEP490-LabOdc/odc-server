package com.odc.checklistservice.service;

import com.odc.checklistservice.dto.request.CreateChecklistTemplateRequest;
import com.odc.checklistservice.dto.request.UpdateChecklistTemplateRequest;
import com.odc.checklistservice.dto.response.GetChecklistTemplateResponse;
import com.odc.checklistservice.entity.ChecklistTemplate;
import com.odc.checklistservice.entity.TemplateGroup;
import com.odc.checklistservice.entity.TemplateItem;
import com.odc.checklistservice.repository.ChecklistTemplateRepository;
import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.common.dto.SortRequest;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.common.specification.GenericSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public ApiResponse<List<GetChecklistTemplateResponse>> searchAllChecklistTemplates(SearchRequest request, Boolean includeDeletedItems) {
        Specification<ChecklistTemplate> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);

        return ApiResponse.success(templateRepository
                .findAll(specification, sort)
                .stream()
                .map(x -> mapEntityToGetResponseDto(x, includeDeletedItems))
                .toList());
    }

    @Override
    public ApiResponse<PaginatedResult<GetChecklistTemplateResponse>> searchChecklistTemplatesWithPagination(SearchRequest request, Boolean includeDeletedItems) {
        Specification<ChecklistTemplate> specification = new GenericSpecification<>(request.getFilters());

        List<Sort.Order> orders = new ArrayList<>();
        if (request.getSorts() != null && !request.getSorts().isEmpty()) {
            for (SortRequest sortRequest : request.getSorts()) {
                orders.add(new Sort.Order(sortRequest.getDirection(), sortRequest.getKey()));
            }
        }
        Sort sort = Sort.by(orders);

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), sort);

        Page<GetChecklistTemplateResponse> page = templateRepository
                .findAll(specification, pageable)
                .map(x -> mapEntityToGetResponseDto(x, includeDeletedItems));

        return ApiResponse.success(PaginatedResult.from(page));
    }

    @Override
    @Transactional
    public ApiResponse<UUID> updateChecklistTemplate(UUID id, UpdateChecklistTemplateRequest request) {

        ChecklistTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy template với ID: " + id));

        template.setName(request.getName());
        template.setDescription(request.getDescription());

        updateGroups(template, request.getGroups());

        templateRepository.save(template);
        return ApiResponse.success("Cập nhật checklist template thành công", template.getId());
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
                        itemResponse.setIsDeleted(item.getIsDeleted());
                        return itemResponse;
                    }).collect(Collectors.toList()));
                }
                return groupResponse;
            }).collect(Collectors.toList()));
        }
        return response;
    }

    private GetChecklistTemplateResponse mapEntityToGetResponseDto(
            ChecklistTemplate entity,
            boolean includeDeletedItems
    ) {
        GetChecklistTemplateResponse response = new GetChecklistTemplateResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setEntityType(entity.getEntityType());
        response.setCreatedAt(entity.getCreatedAt());

        if (entity.getGroups() != null) {
            response.setGroups(
                    entity.getGroups().stream()
                            .map(group -> {
                                GetChecklistTemplateResponse.GroupResponse groupResponse =
                                        new GetChecklistTemplateResponse.GroupResponse();

                                groupResponse.setId(group.getId());
                                groupResponse.setTitle(group.getTitle());
                                groupResponse.setDisplayOrder(group.getDisplayOrder());

                                if (group.getItems() != null) {
                                    groupResponse.setItems(
                                            group.getItems().stream()
                                                    .filter(item ->
                                                            includeDeletedItems ||
                                                                    Boolean.FALSE.equals(item.getIsDeleted())
                                                    )
                                                    .map(item -> {
                                                        GetChecklistTemplateResponse.ItemResponse itemResponse =
                                                                new GetChecklistTemplateResponse.ItemResponse();
                                                        itemResponse.setId(item.getId());
                                                        itemResponse.setContent(item.getContent());
                                                        itemResponse.setDisplayOrder(item.getDisplayOrder());
                                                        itemResponse.setRequired(item.getIsRequired());
                                                        itemResponse.setIsDeleted(item.getIsDeleted());
                                                        return itemResponse;
                                                    })
                                                    .toList()
                                    );
                                }

                                return groupResponse;
                            })
                            .toList()
            );
        }

        return response;
    }

    private void updateGroups(
            ChecklistTemplate template,
            List<UpdateChecklistTemplateRequest.GroupRequest> groupRequests) {

        Map<UUID, TemplateGroup> existingGroups =
                template.getGroups().stream()
                        .filter(g -> !g.getIsDeleted())
                        .collect(Collectors.toMap(TemplateGroup::getId, g -> g));

        List<TemplateGroup> updatedGroups = new ArrayList<>();

        for (var groupReq : groupRequests) {

            TemplateGroup group;

            if (groupReq.getId() != null) {
                // UPDATE
                group = existingGroups.get(groupReq.getId());
                if (group == null) {
                    throw new IllegalStateException("Group không tồn tại: " + groupReq.getId());
                }

                group.setTitle(groupReq.getTitle());
                group.setDisplayOrder(groupReq.getDisplayOrder());

            } else {
                // CREATE
                group = new TemplateGroup();
                group.setTemplate(template);
                group.setTitle(groupReq.getTitle());
                group.setDisplayOrder(groupReq.getDisplayOrder());
            }

            updateItems(group, groupReq.getItems());
            updatedGroups.add(group);
        }

        // Soft delete group bị remove
        template.getGroups().forEach(group -> {
            if (!updatedGroups.contains(group)) {
                group.setIsDeleted(true);
            }
        });

        template.getGroups().addAll(
                updatedGroups.stream()
                        .filter(g -> !template.getGroups().contains(g))
                        .toList()
        );
    }

    private void updateItems(
            TemplateGroup group,
            List<UpdateChecklistTemplateRequest.ItemRequest> itemRequests) {

        Map<UUID, TemplateItem> existingItems =
                group.getItems().stream()
                        .collect(Collectors.toMap(TemplateItem::getId, i -> i));

        for (var itemReq : itemRequests) {

            // FE đánh dấu delete
            if (Boolean.TRUE.equals(itemReq.getIsDeleted())) {

                if (itemReq.getId() != null) {
                    TemplateItem item = existingItems.get(itemReq.getId());
                    if (item != null) {
                        item.setIsDeleted(true);
                    }
                }

                // item mới nhưng delete → bỏ qua
                continue;
            }

            // CREATE
            if (itemReq.getId() == null) {
                TemplateItem item = new TemplateItem();
                item.setGroup(group);
                item.setContent(itemReq.getContent());
                item.setDisplayOrder(itemReq.getDisplayOrder());
                item.setIsRequired(itemReq.isRequired());
                item.setIsDeleted(false);

                group.getItems().add(item);
                continue;
            }

            // UPDATE
            TemplateItem item = existingItems.get(itemReq.getId());
            if (item == null) {
                throw new IllegalStateException("Item không tồn tại: " + itemReq.getId());
            }

            item.setContent(itemReq.getContent());
            item.setDisplayOrder(itemReq.getDisplayOrder());
            item.setIsRequired(itemReq.isRequired());
            item.setIsDeleted(false);
        }
    }

}
