package com.illini.grades.dto;

import java.time.LocalDate;
import java.util.List;

public record ScheduledSectionDto(
    long id,
    String crn,
    String sectionNumber,
    String statusCode,
    String partOfTerm,
    LocalDate startDate,
    LocalDate endDate,
    String notes,
    List<ScheduledSectionMeetingDto> meetings
) {}
