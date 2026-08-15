package com.illini.grades.dto;

public record TermDto(
    long id,
    int year,
    String season,
    String yearTerm,
    long sectionCount,
    boolean isDefault
) {
    public TermDto(long id, int year, String season, String yearTerm) {
        this(id, year, season, yearTerm, 0, false);
    }
}
