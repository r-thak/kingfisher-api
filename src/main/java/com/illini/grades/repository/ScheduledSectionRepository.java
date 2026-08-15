package com.illini.grades.repository;

import com.illini.grades.entity.ScheduledSection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ScheduledSectionRepository extends JpaRepository<ScheduledSection, Long> {
    Optional<ScheduledSection> findByCourseOfferingIdAndCrn(Long courseOfferingId, String crn);

    // Note: can't @EntityGraph both meetings and meetings.instructors together -- Hibernate
    // rejects fetch-joining two List ("bag") collections at once (MultipleBagFetchException).
    // Meetings are fetch-joined; instructors lazy-load per meeting (fine at this data volume).
    @EntityGraph(attributePaths = {"meetings"})
    List<ScheduledSection> findByCourseOfferingIdOrderBySectionNumberAsc(Long courseOfferingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScheduledSection s WHERE s.courseOffering.id = :courseOfferingId AND s.crn NOT IN :crns")
    void deleteStale(@Param("courseOfferingId") Long courseOfferingId, @Param("crns") List<String> crns);

    @Modifying
    @Transactional
    void deleteByCourseOfferingId(Long courseOfferingId);
}
