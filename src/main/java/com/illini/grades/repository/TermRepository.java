package com.illini.grades.repository;

import com.illini.grades.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<Term> findByYearTerm(String yearTerm);
    List<Term> findAllByOrderByYearAscSeasonAsc();
}
