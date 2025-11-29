package com.odc.projectservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScanResultResponse {
    private Boolean isCv;           // true/false
    private String reason;          // Lý do nếu không phải CV
    private Integer matchScore;     // 0-100
    private String summary;         // Nhận xét ngắn
    private List<String> pros;      // Điểm mạnh
    private List<String> cons;      // Điểm yếu
}
