package com.odc.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Đối tượng chứa các tiêu chí tìm kiếm, lọc, và phân trang.")
public class SearchRequest {

    @Schema(
            description = "Danh sách các điều kiện lọc. Đây là trường bắt buộc.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<FilterRequest> filters;

    @Schema(description = "(Tùy chọn) Danh sách các tiêu chí sắp xếp.", nullable = true)
    private List<SortRequest> sorts;

    @Schema(description = "(Tùy chọn) Số trang, bắt đầu từ 1.", nullable = true, example = "1")
    private Integer page;

    @Schema(description = "(Tùy chọn) Số lượng phần tử trên mỗi trang.", nullable = true, example = "10")
    private Integer size;
}