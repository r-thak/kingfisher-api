package com.illini.grades.controller;

import com.illini.grades.dto.TermDto;
import com.illini.grades.service.TermService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/terms")
@Tag(name = "Terms")
public class TermController {

    private final TermService termService;

    public TermController(TermService termService) {
        this.termService = termService;
    }

    @GetMapping
    public List<TermDto> listTerms() {
        return termService.listTerms();
    }
}
