package com.illini.grades.dto;

public record SectionGradeDto(
    long sectionId,
    String schedType,
    InstructorSummaryDto instructor,
    GradeDistributionDto grades
) {}
