package com.illini.grades.repository;

import com.illini.grades.entity.InstructorGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstructorGradeRepository extends JpaRepository<InstructorGrade, Long>, JpaSpecificationExecutor<InstructorGrade> {
}
