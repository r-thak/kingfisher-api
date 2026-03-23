package com.illini.grades.dto;

import java.util.List;

public record CourseOfferingDetailDto(
    long id,
    CourseSummaryDto course,
    TermDto term,
    List<SectionGradeDto> sections
) {}
