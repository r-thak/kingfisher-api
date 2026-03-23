package com.illini.grades.controller;

import com.illini.grades.dto.CourseDetailDto;
import com.illini.grades.dto.CourseGradesResponseDto;
import com.illini.grades.dto.CourseSummaryDto;
import com.illini.grades.dto.PagedResponse;
import com.illini.grades.service.CourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/courses")
@Tag(name = "Courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public PagedResponse<CourseSummaryDto> listCourses(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) Long instructor,
            @RequestParam(required = false) Integer number,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int perPage,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String order) {
        return courseService.listCourses(query, subject, instructor, number, page, perPage, sort, order);
    }

    @GetMapping("/{id}")
    public CourseDetailDto getCourse(@PathVariable Long id) {
        return courseService.getCourse(id);
    }

    @GetMapping("/{id}/grades")
    public CourseGradesResponseDto getCourseGrades(@PathVariable Long id) {
        return courseService.getCourseGrades(id);
    }
}
