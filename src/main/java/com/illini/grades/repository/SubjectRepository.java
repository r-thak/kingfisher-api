package com.illini.grades.repository;

import com.illini.grades.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByCode(String code);
    List<Subject> findAllByOrderByCodeAsc();
}
