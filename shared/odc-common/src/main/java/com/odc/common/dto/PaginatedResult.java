package com.odc.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResult<T> {
    private List<T> data;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PaginatedResult<T> from(Page<T> page) {
        return PaginatedResult.<T>builder()
                .data(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber() + 1)
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
