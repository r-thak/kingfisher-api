package com.illini.grades.dto;

public record CourseOfferingSummaryDto(
    long id,
    TermDto term,
    String url
) {}
