package com.odc.common.dto;

import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class SortRequest {
    private String key;
    private Sort.Direction direction = Sort.Direction.ASC;
}
