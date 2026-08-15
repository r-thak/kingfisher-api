package com.illini.grades.dto;

import java.util.List;

public record PagedResponse<T>(
    int currentPage,
    int totalPages,
    long totalCount,
    List<T> results,
    List<String> availableCohorts
) {
    public PagedResponse(int currentPage, int totalPages, long totalCount, List<T> results) {
        this(currentPage, totalPages, totalCount, results, List.of());
    }
}
