package com.odc.emailservice.service;

import java.util.Map;

public interface EmailService {
    void sendEmailWithHtmlTemplate(String to, String subject, String templateName, Map<String, Object> templateModel);
}
