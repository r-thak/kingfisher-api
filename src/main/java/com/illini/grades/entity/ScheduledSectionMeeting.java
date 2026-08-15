package com.illini.grades.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scheduled_section_meetings")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledSectionMeeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_section_id", nullable = false)
    private ScheduledSection scheduledSection;

    @Column(name = "meeting_index", nullable = false)
    private Short meetingIndex;

    @Column(name = "start_time", length = 10)
    private String startTime;

    @Column(name = "end_time", length = 10)
    private String endTime;

    @Column(name = "days_of_week", length = 10)
    private String daysOfWeek;

    @Column(name = "room_number", length = 50)
    private String roomNumber;

    @Column(name = "building_name", length = 255)
    private String buildingName;

    @Column(name = "type_code", length = 10)
    private String typeCode;

    @Column(name = "type_description", length = 100)
    private String typeDescription;

    @ManyToMany
    @JoinTable(
        name = "scheduled_section_meeting_instructors",
        joinColumns = @JoinColumn(name = "meeting_id"),
        inverseJoinColumns = @JoinColumn(name = "instructor_id")
    )
    private List<Instructor> instructors = new ArrayList<>();
}
