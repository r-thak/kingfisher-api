package com.illini.grades.repository;

import com.illini.grades.entity.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    Optional<Instructor> findByName(String name);

    @Query(value = """
        SELECT i.* FROM instructors i
        WHERE i.name % :query
        ORDER BY similarity(i.name, :query) DESC
        """,
        countQuery = """
        SELECT count(*) FROM instructors i
        WHERE i.name % :query
        """, nativeQuery = true)
    Page<Instructor> searchByQuery(@Param("query") String query, Pageable pageable);
}
