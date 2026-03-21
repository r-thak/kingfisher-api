package com.illini.grades.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @Column(name = "sched_type", nullable = false, length = 10)
    private String schedType;

    @Column(name = "a_plus", nullable = false)
    private Short aPlus = 0;

    @Column(nullable = false)
    private Short a = 0;

    @Column(name = "a_minus", nullable = false)
    private Short aMinus = 0;

    @Column(name = "b_plus", nullable = false)
    private Short bPlus = 0;

    @Column(nullable = false)
    private Short b = 0;

    @Column(name = "b_minus", nullable = false)
    private Short bMinus = 0;

    @Column(name = "c_plus", nullable = false)
    private Short cPlus = 0;

    @Column(nullable = false)
    private Short c = 0;

    @Column(name = "c_minus", nullable = false)
    private Short cMinus = 0;

    @Column(name = "d_plus", nullable = false)
    private Short dPlus = 0;

    @Column(nullable = false)
    private Short d = 0;

    @Column(name = "d_minus", nullable = false)
    private Short dMinus = 0;

    @Column(nullable = false)
    private Short f = 0;

    @Column(nullable = false)
    private Short w = 0;

    @Column(nullable = false)
    private Short students = 0;
}
