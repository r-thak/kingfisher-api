package com.illini.grades.controller;

import com.illini.grades.dto.SectionDetailDto;
import com.illini.grades.service.SectionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/sections")
@Tag(name = "Sections")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping("/{id}")
    public SectionDetailDto getSection(@PathVariable Long id) {
        return sectionService.getSection(id);
    }
}
