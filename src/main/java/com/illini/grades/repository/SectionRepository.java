package com.illini.grades.repository;

import com.illini.grades.entity.Section;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    Optional<Section> findByCourseOfferingIdAndInstructorIdAndSchedType(Long courseOfferingId, Long instructorId, String schedType);
    
    @EntityGraph(attributePaths = {"instructor"})
    List<Section> findByCourseOfferingId(Long courseOfferingId);
    
    @EntityGraph(attributePaths = {"courseOffering", "courseOffering.course", "courseOffering.term", "courseOffering.course.subject"})
    List<Section> findByInstructorId(Long instructorId);
    
    @EntityGraph(attributePaths = {"instructor"})
    List<Section> findByCourseOfferingIdIn(List<Long> courseOfferingIds);
}
