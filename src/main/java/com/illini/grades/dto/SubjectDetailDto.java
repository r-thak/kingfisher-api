package com.illini.grades.dto;

import java.util.List;

public record SubjectDetailDto(
    long id,
    String code,
    List<CourseSummaryDto> courses
) {}
