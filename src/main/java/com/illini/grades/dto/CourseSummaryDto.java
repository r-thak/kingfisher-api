package com.illini.grades.dto;

public record CourseSummaryDto(
    long id,
    SubjectSummaryDto subject,
    int number,
    String title,
    String url
) {}
