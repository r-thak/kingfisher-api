package com.illini.grades.dto;

public record TermDto(
    long id,
    int year,
    String season,
    String yearTerm
) {}
