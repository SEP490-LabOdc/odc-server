package com.odc.projectservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.projectservice.dto.request.CreateSkillRequest;
import com.odc.projectservice.dto.request.UpdateSkillRequest;
import com.odc.projectservice.dto.response.SkillResponse;
import com.odc.projectservice.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;

    @PostMapping
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        ApiResponse<SkillResponse> response = skillService.createSkill(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSkillRequest request) {
        return ResponseEntity.ok(skillService.updateSkill(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> deleteSkill(@PathVariable UUID id) {
        ApiResponse<SkillResponse> response = skillService.deleteSkill(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getAllSkills() {
        ApiResponse<List<SkillResponse>> response = skillService.getAllSkills();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> getSkillById(@PathVariable UUID id) {
        ApiResponse<SkillResponse> response = skillService.getSkillById(id);
        return ResponseEntity.ok(response);
    }
}
