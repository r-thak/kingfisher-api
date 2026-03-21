package com.illini.grades.repository;

import com.illini.grades.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
    Optional<CourseOffering> findByCourseIdAndTermId(Long courseId, Long termId);
    List<CourseOffering> findByCourseId(Long courseId);
    List<CourseOffering> findByTermId(Long termId);
}
