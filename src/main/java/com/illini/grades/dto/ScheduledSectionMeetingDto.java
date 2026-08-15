package com.illini.grades.dto;

import java.util.List;

public record ScheduledSectionMeetingDto(
    String startTime,
    String endTime,
    String daysOfWeek,
    String roomNumber,
    String buildingName,
    String typeCode,
    String typeDescription,
    List<InstructorSummaryDto> instructors
) {}
