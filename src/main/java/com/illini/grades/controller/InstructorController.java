package com.illini.grades.controller;

import com.illini.grades.dto.InstructorDetailDto;
import com.illini.grades.dto.InstructorGradesResponseDto;
import com.illini.grades.dto.InstructorSummaryDto;
import com.illini.grades.dto.PagedResponse;
import com.illini.grades.service.InstructorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/instructors")
@Tag(name = "Instructors")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @GetMapping
    public PagedResponse<InstructorSummaryDto> listInstructors(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int perPage) {
        return instructorService.listInstructors(query, page, perPage);
    }

    @GetMapping("/{id}")
    public InstructorDetailDto getInstructor(@PathVariable Long id) {
        return instructorService.getInstructor(id);
    }

    @GetMapping("/{id}/grades")
    public InstructorGradesResponseDto getInstructorGrades(@PathVariable Long id) {
        return instructorService.getInstructorGrades(id);
    }
}
