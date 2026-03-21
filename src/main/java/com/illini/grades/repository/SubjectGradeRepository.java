package com.illini.grades.repository;

import com.illini.grades.entity.SubjectGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubjectGradeRepository extends JpaRepository<SubjectGrade, Long>, JpaSpecificationExecutor<SubjectGrade> {
}
