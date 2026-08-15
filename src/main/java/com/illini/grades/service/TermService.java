package com.illini.grades.service;

import com.illini.grades.dto.TermDto;
import com.illini.grades.repository.TermRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TermService {

    private final TermRepository termRepository;
    private final EntityManager entityManager;

    public TermService(TermRepository termRepository, EntityManager entityManager) {
        this.termRepository = termRepository;
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<TermDto> listTerms() {
        String sql = """
            SELECT t.id, t.year, t.season, t.year_term,
                   COALESCE(count(ss.id), 0) as section_count
            FROM terms t
            LEFT JOIN course_offerings co ON co.term_id = t.id
            LEFT JOIN scheduled_sections ss ON ss.course_offering_id = co.id
            GROUP BY t.id, t.year, t.season, t.year_term
            ORDER BY t.year DESC,
                     CASE LOWER(t.season)
                         WHEN 'winter' THEN 4
                         WHEN 'fall' THEN 3
                         WHEN 'summer' THEN 2
                         WHEN 'spring' THEN 1
                         ELSE 0
                     END DESC
        """;

        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        
        // Find default: the newest semester with the most course section data (> 0)
        long maxCount = -1L;
        Long defaultId = null;
        for (Object[] r : rows) {
            long count = ((Number) r[4]).longValue();
            if (count > maxCount && count > 0) {
                maxCount = count;
                defaultId = ((Number) r[0]).longValue();
            }
        }
        if (defaultId == null && !rows.isEmpty()) {
            defaultId = ((Number) rows.get(0)[0]).longValue();
        }

        List<TermDto> dtos = new ArrayList<>();
        for (Object[] r : rows) {
            long id = ((Number) r[0]).longValue();
            int year = ((Number) r[1]).intValue();
            String season = (String) r[2];
            String yearTerm = (String) r[3];
            long count = ((Number) r[4]).longValue();
            boolean isDefault = (defaultId != null && defaultId == id);
            dtos.add(new TermDto(id, year, season, yearTerm, count, isDefault));
        }

        return dtos;
    }
}
