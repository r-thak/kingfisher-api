package com.illini.grades.controller;

import com.illini.grades.dto.SubjectDetailDto;
import com.illini.grades.dto.SubjectSummaryDto;
import com.illini.grades.service.SubjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/subjects")
@Tag(name = "Subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public List<SubjectSummaryDto> listSubjects() {
        return subjectService.listSubjects();
    }

    @GetMapping("/{id}")
    public SubjectDetailDto getSubject(@PathVariable Long id) {
        return subjectService.getSubject(id);
    }
}
