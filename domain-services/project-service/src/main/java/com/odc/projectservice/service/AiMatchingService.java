package com.odc.projectservice.service;

import java.util.Map;

public interface AiMatchingService {
    Map<String, Object> analyzeCv(String cvUrl, String jobDescription);
}
