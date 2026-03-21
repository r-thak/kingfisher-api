package com.illini.grades.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subject_grades")
@Getter
@Setter
@NoArgsConstructor
public class SubjectGrade {
    @Id
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "total_students", nullable = false)
    private Integer totalStudents;

    @Column(name = "avg_students", nullable = false)
    private Double avgStudents;

    @Column(nullable = false)
    private Double gpa;
}
