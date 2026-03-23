package com.illini.grades.dto;

public record ExploreInstructorDto(
    InstructorSummaryDto instructor,
    int totalStudents,
    double avgStudents,
    double gpa
) {}
