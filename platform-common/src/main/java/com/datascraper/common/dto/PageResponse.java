package com.datascraper.common.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        boolean hasMore
) {
    public static <T> PageResponse<T> of(List<T> items, int page, int pageSize, long total) {
        boolean hasMore = (long) (page + 1) * pageSize < total;
        return new PageResponse<>(items, page, pageSize, total, hasMore);
    }
}
