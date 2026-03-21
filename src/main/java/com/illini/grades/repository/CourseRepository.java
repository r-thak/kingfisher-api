package com.illini.grades.repository;

import com.illini.grades.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    Optional<Course> findBySubjectIdAndNumber(Long subjectId, Short number);

    @Query(value = """
        SELECT c.* FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
        ORDER BY GREATEST(
            similarity(c.title, :query),
            similarity(s.code || ' ' || c.number::text, :query)
        ) DESC
        """,
        countQuery = """
        SELECT count(*) FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
        """, nativeQuery = true)
    Page<Course> searchByQuery(@Param("query") String query, Pageable pageable);
}
