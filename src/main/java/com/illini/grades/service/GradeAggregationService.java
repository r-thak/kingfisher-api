package com.illini.grades.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeAggregationService {

    private final EntityManager entityManager;

    public GradeAggregationService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void recomputeAll() {
        entityManager.createNativeQuery("TRUNCATE TABLE course_grades, subject_grades, instructor_grades").executeUpdate();

        entityManager.createNativeQuery("""
            INSERT INTO course_grades (course_id, total_students, avg_students, gpa)
            SELECT
                co.course_id,
                COALESCE(SUM(s.students), 0) as total_students,
                COALESCE(CAST(SUM(s.students) AS NUMERIC) / NULLIF(COUNT(DISTINCT co.id), 0), 0) as avg_students,
                COALESCE(
                    (4.0 * SUM(s.a_plus + s.a) + 3.67 * SUM(s.a_minus) + 3.33 * SUM(s.b_plus) +
                     3.0 * SUM(s.b) + 2.67 * SUM(s.b_minus) + 2.33 * SUM(s.c_plus) +
                     2.0 * SUM(s.c) + 1.67 * SUM(s.c_minus) + 1.33 * SUM(s.d_plus) +
                     1.0 * SUM(s.d) + 0.67 * SUM(s.d_minus) + 0.0 * SUM(s.f))
                    / NULLIF(SUM(s.a_plus + s.a + s.a_minus + s.b_plus + s.b + s.b_minus + s.c_plus + s.c + s.c_minus + s.d_plus + s.d + s.d_minus + s.f), 0),
                    0.0
                ) as gpa
            FROM sections s
            JOIN course_offerings co ON s.course_offering_id = co.id
            GROUP BY co.course_id
            """).executeUpdate();

        entityManager.createNativeQuery("""
            INSERT INTO subject_grades (subject_id, total_students, avg_students, gpa)
            SELECT
                c.subject_id,
                COALESCE(SUM(s.students), 0) as total_students,
                COALESCE(CAST(SUM(s.students) AS NUMERIC) / NULLIF(COUNT(DISTINCT co.id), 0), 0) as avg_students,
                COALESCE(
                    (4.0 * SUM(s.a_plus + s.a) + 3.67 * SUM(s.a_minus) + 3.33 * SUM(s.b_plus) +
                     3.0 * SUM(s.b) + 2.67 * SUM(s.b_minus) + 2.33 * SUM(s.c_plus) +
                     2.0 * SUM(s.c) + 1.67 * SUM(s.c_minus) + 1.33 * SUM(s.d_plus) +
                     1.0 * SUM(s.d) + 0.67 * SUM(s.d_minus) + 0.0 * SUM(s.f))
                    / NULLIF(SUM(s.a_plus + s.a + s.a_minus + s.b_plus + s.b + s.b_minus + s.c_plus + s.c + s.c_minus + s.d_plus + s.d + s.d_minus + s.f), 0),
                    0.0
                ) as gpa
            FROM sections s
            JOIN course_offerings co ON s.course_offering_id = co.id
            JOIN courses c ON co.course_id = c.id
            GROUP BY c.subject_id
            """).executeUpdate();

        entityManager.createNativeQuery("""
            INSERT INTO instructor_grades (instructor_id, total_students, avg_students, gpa)
            SELECT
                s.instructor_id,
                COALESCE(SUM(s.students), 0) as total_students,
                COALESCE(CAST(SUM(s.students) AS NUMERIC) / NULLIF(COUNT(DISTINCT s.id), 0), 0) as avg_students,
                COALESCE(
                    (4.0 * SUM(s.a_plus + s.a) + 3.67 * SUM(s.a_minus) + 3.33 * SUM(s.b_plus) +
                     3.0 * SUM(s.b) + 2.67 * SUM(s.b_minus) + 2.33 * SUM(s.c_plus) +
                     2.0 * SUM(s.c) + 1.67 * SUM(s.c_minus) + 1.33 * SUM(s.d_plus) +
                     1.0 * SUM(s.d) + 0.67 * SUM(s.d_minus) + 0.0 * SUM(s.f))
                    / NULLIF(SUM(s.a_plus + s.a + s.a_minus + s.b_plus + s.b + s.b_minus + s.c_plus + s.c + s.c_minus + s.d_plus + s.d + s.d_minus + s.f), 0),
                    0.0
                ) as gpa
            FROM sections s
            GROUP BY s.instructor_id
            """).executeUpdate();
    }
}
