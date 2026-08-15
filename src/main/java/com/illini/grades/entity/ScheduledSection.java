package com.illini.grades.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scheduled_sections")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Column(nullable = false, length = 20)
    private String crn;

    @Column(name = "section_number", length = 10)
    private String sectionNumber;

    @Column(name = "status_code", length = 10)
    private String statusCode;

    @Column(name = "part_of_term", length = 10)
    private String partOfTerm;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "scheduledSection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduledSectionMeeting> meetings = new ArrayList<>();
}
