package com.illini.grades.dto;

import java.util.List;

public record CourseOfferingGradeDto(
    long termId,
    String yearTerm,
    GradeDistributionDto cumulative,
    List<SectionGradeDto> sections
) {}
