package com.illini.grades.dto;

public record CourseSummaryDto(
    long id,
    SubjectSummaryDto subject,
    int number,
    String title,
    Double gpa,
    Integer totalStudents,
    String url
) {}
