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
    private final RestTemplate externalRestTemplate;
    private final ObjectMapper objectMapper;
    @Value("${gemini.api.key}")
    private String apiKey;
    @Value("${gemini.base-url}")
    private String baseUrl;

    public Map<String, Object> analyzeCv(String cvUrl, String jobDescription) {
        try {
            byte[] pdfBytes;
            try (InputStream in = new BufferedInputStream(new URL(cvUrl).openStream())) {
                pdfBytes = in.readAllBytes();
            }
            String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

            // --- CẢI TIẾN PROMPT ---
            // Thay đổi chiến lược: Định nghĩa cấu trúc JSON chuẩn và bắt buộc AI tuân thủ giá trị mặc định (Default Values)
            String prompt = String.format("""
                    Bạn là một chuyên gia tuyển dụng AI (AI Recruiter). Nhiệm vụ của bạn là phân tích file đính kèm dựa trên Mô tả công việc (JD) được cung cấp.
                    
                    --- MÔ TẢ CÔNG VIỆC (JD) ---
                    %s
                    -----------------------------
                    
                    HÃY TRẢ LỜI BẰNG DUY NHẤT MỘT JSON HỢP LỆ. KHÔNG DÙNG MARKDOWN (```json).
                    TUÂN THỦ TUYỆT ĐỐI CẤU TRÚC VÀ GIÁ TRỊ MẶC ĐỊNH SAU ĐÂY:
                    
                    1. KIỂM TRA FILE:
                       - Nếu file KHÔNG PHẢI là CV (Hóa đơn, sách, code, ảnh rác, quá ngắn...):
                         -> Set "is_cv": false
                         -> Set "match_score": 0
                         -> Set "reason": "Lý do cụ thể tại sao không phải CV"
                         -> Các trường khác giữ nguyên giá trị mặc định rỗng.
                    
                    2. NẾU LÀ CV:
                       - So khớp kỹ năng trong CV với JD.
                       - Set "is_cv": true
                       - Tính toán "match_score" (0-100).
                       - Điền các trường còn lại.
                    
                    3. CẤU TRÚC JSON BẮT BUỘC (Mọi trường hợp đều phải trả về đủ các key này, KHÔNG được null):
                    {
                        "is_cv": boolean,           // Mặc định: false
                        "match_score": integer,     // Mặc định: 0 (Nếu is_cv=false thì bắt buộc là 0)
                        "reason": string,           // Mặc định: "" (Nếu is_cv=true thì để chuỗi rỗng)
                        "summary": string,          // Mặc định: "" (Nếu is_cv=false thì để chuỗi rỗng)
                        "pros": [string],           // Mặc định: [] (Mảng rỗng nếu không có dữ liệu)
                        "cons": [string]            // Mặc định: [] (Mảng rỗng nếu không có dữ liệu)
                    }
                    """, jobDescription);

            String apiUrl = baseUrl + apiKey;

            // Cấu hình GenerationConfig để ép kiểu trả về là JSON (Nếu model hỗ trợ)
            // Lưu ý: Gemini Flash 1.5/Pro 1.5 hỗ trợ responseMimeType: "application/json"
            Map<String, Object> generationConfig = Map.of(
                    "response_mime_type", "application/json"
            );

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
                    ),
                    // Thêm config này để tăng độ ổn định của JSON
                    "generation_config", generationConfig
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            var response = externalRestTemplate.postForEntity(apiUrl, new HttpEntity<>(payload, headers), Map.class);

            return extractContent(response.getBody());

        } catch (Exception e) {
            log.error("Lỗi khi gọi AI: {}", e.getMessage());
            // Fallback an toàn cho code Java nếu crash
            return Map.of(
                    "is_cv", false,
                    "match_score", 0,
                    "reason", "Lỗi hệ thống AI: " + e.getMessage(),
                    "summary", "",
                    "pros", List.of(),
                    "cons", List.of()
            );
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
