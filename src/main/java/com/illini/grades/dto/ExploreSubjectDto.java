package com.illini.grades.dto;

public record ExploreSubjectDto(
    SubjectSummaryDto subject,
    int totalStudents,
    double avgStudents,
    double gpa
) {}
