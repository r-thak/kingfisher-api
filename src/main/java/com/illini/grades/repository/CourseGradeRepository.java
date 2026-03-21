package com.illini.grades.repository;

import com.illini.grades.entity.CourseGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseGradeRepository extends JpaRepository<CourseGrade, Long>, JpaSpecificationExecutor<CourseGrade> {
}
