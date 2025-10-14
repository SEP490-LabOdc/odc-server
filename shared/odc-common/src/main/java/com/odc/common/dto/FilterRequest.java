package com.odc.common.dto;

import com.odc.common.constant.Operator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FilterRequest {
    @Schema(description = "Tên trường của entity cần lọc.", example = "entityType")
    private String key; // (Ex: "name", "price")

    @Schema(description = "Toán tử so sánh.", example = "EQUAL")
    private Operator operator; // (Ex: EQUAL, LIKE, GREATER_THAN)

    @Schema(description = "Giá trị cần so sánh.", example = "COMPANY_REGISTRATION")
    private Object value;

    @Schema(description = "Giá trị cần so sánh khi dùng với toán tử BETWEEN.")
    private Object valueTo; // (Ex: price BETWEEN 100 AND 200)
}
