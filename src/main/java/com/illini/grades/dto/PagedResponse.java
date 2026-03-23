package com.illini.grades.dto;

import java.util.List;

public record PagedResponse<T>(
    int currentPage,
    int totalPages,
    long totalCount,
    List<T> results
) {}
