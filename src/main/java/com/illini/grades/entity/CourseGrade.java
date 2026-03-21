package com.illini.grades.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_grades")
@Getter
@Setter
@NoArgsConstructor
public class CourseGrade {
    @Id
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "total_students", nullable = false)
    private Integer totalStudents;

    @Column(name = "avg_students", nullable = false)
    private Double avgStudents;

    @Column(nullable = false)
    private Double gpa;
}
