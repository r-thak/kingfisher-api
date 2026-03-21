package com.illini.grades.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instructor_grades")
@Getter
@Setter
@NoArgsConstructor
public class InstructorGrade {
    @Id
    @Column(name = "instructor_id")
    private Long instructorId;

    @Column(name = "total_students", nullable = false)
    private Integer totalStudents;

    @Column(name = "avg_students", nullable = false)
    private Double avgStudents;

    @Column(nullable = false)
    private Double gpa;
}
