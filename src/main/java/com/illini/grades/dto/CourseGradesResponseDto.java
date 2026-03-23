package com.illini.grades.dto;

import java.util.List;

public record CourseGradesResponseDto(
    long courseId,
    GradeDistributionDto cumulative,
    List<CourseOfferingGradeDto> courseOfferings
) {}
