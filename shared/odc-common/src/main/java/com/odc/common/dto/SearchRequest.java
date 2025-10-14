package com.odc.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class SearchRequest {
    private List<FilterRequest> filters;
    private List<SortRequest> sorts;
    private Integer page;
    private Integer size;
}