package com.illini.grades.dto;

import java.util.List;

public record CourseDetailDto(
    long id,
    SubjectSummaryDto subject,
    int number,
    String title,
    String gradesUrl,
    List<CourseOfferingSummaryDto> courseOfferings
) {}
