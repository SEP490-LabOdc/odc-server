package com.odc.projectservice.service;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.common.dto.SearchRequest;
import com.odc.projectservice.dto.request.CreateSkillRequest;
import com.odc.projectservice.dto.request.UpdateSkillRequest;
import com.odc.projectservice.dto.response.SkillResponse;

import java.util.List;
import java.util.UUID;

public interface SkillService {
    ApiResponse<SkillResponse> createSkill(CreateSkillRequest request);

    ApiResponse<SkillResponse> updateSkill(UUID id, UpdateSkillRequest request);

    ApiResponse<SkillResponse> deleteSkill(UUID id);

    ApiResponse<List<SkillResponse>> getAllSkills();

    ApiResponse<SkillResponse> getSkillById(UUID id);

    ApiResponse<List<SkillResponse>> searchSkills(SearchRequest request);

    ApiResponse<PaginatedResult<SkillResponse>> searchSkillsWithPagination(SearchRequest request);
}
