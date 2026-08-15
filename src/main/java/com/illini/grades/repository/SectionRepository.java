package com.illini.grades.repository;

import com.illini.grades.entity.Section;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    // sched_type casing is inconsistent in the source CSV data (e.g. "ONL" vs "Onl"),
    // so codes are normalized to uppercase here rather than treated as distinct types.
    @Query(value = """
        SELECT UPPER(sched_type) AS code, COUNT(*) AS cnt
        FROM sections
        WHERE sched_type IS NOT NULL AND sched_type <> ''
        GROUP BY UPPER(sched_type)
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<Object[]> countDistinctSchedTypes();
}
