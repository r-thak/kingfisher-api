package com.illini.grades.dto;

import java.util.List;

public record InstructorDetailDto(
    long id,
    String name,
    List<CourseSummaryDto> courses
) {}
