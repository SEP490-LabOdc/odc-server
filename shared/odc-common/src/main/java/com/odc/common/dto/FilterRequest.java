package com.odc.common.dto;

import com.odc.common.constant.Operator;
import lombok.Data;

@Data
public class FilterRequest {
    private String key; // (Ex: "name", "price")
    private Operator operator; // (Ex: EQUAL, LIKE, GREATER_THAN)
    private Object value;
    private Object valueTo; // (Ex: price BETWEEN 100 AND 200)
}
