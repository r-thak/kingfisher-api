package com.illini.grades.dto;

public record SectionDetailDto(
    long id,
    CourseOfferingSummaryDto courseOffering,
    CourseSummaryDto course,
    InstructorSummaryDto instructor,
    String schedType,
    GradeDistributionDto grades
) {}
