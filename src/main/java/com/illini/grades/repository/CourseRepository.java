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

    @Query(value = """
        SELECT c.* FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        LEFT JOIN course_grades cg ON c.id = cg.course_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
        ORDER BY cg.avg_students DESC NULLS LAST
        """,
        countQuery = """
        SELECT count(*) FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
        """, nativeQuery = true)
    Page<Course> searchByQueryOrderByPopularity(@Param("query") String query, Pageable pageable);

    @Query(value = """
        SELECT c.* FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        LEFT JOIN course_grades cg ON c.id = cg.course_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
        ORDER BY cg.total_students DESC NULLS LAST
        """,
        countQuery = """
        SELECT count(*) FROM courses c
        JOIN subjects s ON s.id = c.subject_id
        WHERE c.title % :query
           OR (s.code || ' ' || c.number::text) % :query
           OR (s.code || c.number::text) % :query
        """, nativeQuery = true)
    Page<Course> searchByQueryOrderByTotalGrades(@Param("query") String query, Pageable pageable);
}
