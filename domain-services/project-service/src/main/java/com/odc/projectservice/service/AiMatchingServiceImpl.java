package com.odc.projectservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiMatchingServiceImpl implements AiMatchingService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> analyzeCv(String cvUrl, String jobDescription) {
        try {
            byte[] pdfBytes;
            try (InputStream in = new BufferedInputStream(new URL(cvUrl).openStream())) {
                pdfBytes = in.readAllBytes();
            }
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

            // --- PROMPT CẢI TIẾN ---
            // Logic: Check trước -> Nếu sai trả về false ngay -> Nếu đúng mới phân tích
            String prompt = String.format("""
                    Bạn là trợ lý tuyển dụng AI chuyên nghiệp.
                    Nhiệm vụ: Kiểm tra xem file đính kèm có phải là CV/Hồ sơ xin việc (Resume) hợp lệ không.
                    
                    [LOGIC XỬ LÝ]:
                    1. Nếu file KHÔNG PHẢI là CV (ví dụ: hóa đơn, sách, code, hình ảnh không liên quan, văn bản quá ngắn...):
                       -> Trả về JSON: {"is_cv": false, "reason": "Lý do tại sao không phải CV"}
                       -> Dừng lại, KHÔNG phân tích gì thêm.
                    
                    2. Nếu file LÀ CV:
                       -> Hãy so sánh kỹ năng trong CV với JD dưới đây:
                       ---
                       %s
                       ---
                       -> Trả về JSON:
                       {
                           "is_cv": true,
                           "match_score": (số nguyên 0-100),
                           "summary": "Nhận xét ngắn gọn (dưới 50 từ) cho Mentor",
                           "pros": ["điểm mạnh 1", "điểm mạnh 2"],
                           "cons": ["điểm yếu 1", "điểm yếu 2"]
                       }
                    
                    Yêu cầu bắt buộc: Chỉ trả về thuần JSON, không Markdown.
                    """, jobDescription);

            String apiUrl = baseUrl + apiKey;

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt),
                                            Map.of("inline_data", Map.of(
                                                    "mime_type", "application/pdf",
                                                    "data", pdfBase64
                                            ))
                                    )
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = restTemplate.postForEntity(apiUrl, new HttpEntity<>(payload, headers), Map.class);

            return extractContent(response.getBody());
        } catch (Exception e) {
            log.error("Lỗi khi gọi AI: {}", e.getMessage());
            return Map.of("is_cv", false, "reason", "Lỗi hệ thống AI không thể xử lý file này.");
        }
    }

    private Map<String, Object> extractContent(Map<String, Object> responseBody) {
        try {
            List candidates = (List) responseBody.get("candidates");
            Map content = (Map) ((Map) candidates.get(0)).get("content");
            List parts = (List) content.get("parts");
            String jsonText = (String) ((Map) parts.get(0)).get("text");

            jsonText = jsonText.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(jsonText, Map.class);
        } catch (Exception e) {
            log.error("Lỗi parse JSON từ AI response", e);
            return Map.of("is_cv", false, "reason", "Không thể đọc kết quả từ AI");
        }
    }
}
