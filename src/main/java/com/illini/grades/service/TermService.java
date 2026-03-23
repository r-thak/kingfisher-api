package com.illini.grades.service;

import com.illini.grades.dto.TermDto;
import com.illini.grades.repository.TermRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TermService {

    private final TermRepository termRepository;

    public TermService(TermRepository termRepository) {
        this.termRepository = termRepository;
    }

    public List<TermDto> listTerms() {
        return termRepository.findAllByOrderByYearAscSeasonAsc()
                .stream()
                .map(t -> new TermDto(t.getId(), t.getYear(), t.getSeason(), t.getYearTerm()))
                .collect(Collectors.toList());
    }
}
