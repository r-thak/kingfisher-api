package com.illini.grades.dto;

public record ExploreCourseDto(
    CourseSummaryDto course,
    int totalStudents,
    double avgStudents,
    double gpa
) {}
