package com.illini.grades.repository;

import com.illini.grades.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    Optional<Course> findBySubjectIdAndNumber(Long subjectId, Short number);
    List<Course> findByTitleAndNumber(String title, Short number);

    @Query(value = """
        SELECT c.* FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
           OR LOWER(c.title) LIKE '%' || :query || '%'
           OR LOWER(s.code || ' ' || c.number::text) LIKE '%' || :query || '%'
        ORDER BY (
            SELECT CAST(SUM(sec.students) AS FLOAT) / NULLIF(COUNT(DISTINCT co.term_id), 0)
            FROM course_offerings co
            JOIN sections sec ON sec.course_offering_id = co.id
            JOIN terms t ON t.id = co.term_id
            WHERE co.course_id = c.id
              AND t.year >= EXTRACT(YEAR FROM CURRENT_DATE) - 5
        ) DESC NULLS LAST
        """,
        countQuery = """
        SELECT count(*) FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
           OR LOWER(c.title) LIKE '%' || :query || '%'
           OR LOWER(s.code || ' ' || c.number::text) LIKE '%' || :query || '%'
        """, nativeQuery = true)
    Page<Course> searchByQueryOrderByPopularity(@Param("query") String query, Pageable pageable);

    @Query(value = """
        SELECT c.* FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        LEFT JOIN course_grades cg ON c.id = cg.course_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
           OR LOWER(c.title) LIKE '%' || :query || '%'
           OR LOWER(s.code || ' ' || c.number::text) LIKE '%' || :query || '%'
        ORDER BY cg.total_students DESC NULLS LAST
        """,
        countQuery = """
        SELECT count(*) FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
           OR LOWER(c.title) LIKE '%' || :query || '%'
           OR LOWER(s.code || ' ' || c.number::text) LIKE '%' || :query || '%'
        """, nativeQuery = true)
    Page<Course> searchByQueryOrderByTotalGrades(@Param("query") String query, Pageable pageable);

    @Query(value = """
        SELECT c.* FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        LEFT JOIN course_grades cg ON c.id = cg.course_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
           OR LOWER(c.title) LIKE '%' || :query || '%'
           OR LOWER(s.code || ' ' || c.number::text) LIKE '%' || :query || '%'
        ORDER BY cg.gpa DESC NULLS LAST
        """,
        countQuery = """
        SELECT count(*) FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
           OR LOWER(c.title) LIKE '%' || :query || '%'
           OR LOWER(s.code || ' ' || c.number::text) LIKE '%' || :query || '%'
        """, nativeQuery = true)
    Page<Course> searchByQueryOrderByGpa(@Param("query") String query, Pageable pageable);
}
