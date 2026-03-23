package com.illini.grades.dto;

import java.util.List;

public record InstructorGradesResponseDto(
    long instructorId,
    GradeDistributionDto cumulative,
    List<InstructorCourseOfferingGradeDto> courseOfferings
) {
    public record InstructorCourseOfferingGradeDto(
        long termId,
        String yearTerm,
        CourseSummaryDto course,
        long courseOfferingId,
        GradeDistributionDto cumulative,
        List<SectionGradeDto> sections
    ) {}
}
