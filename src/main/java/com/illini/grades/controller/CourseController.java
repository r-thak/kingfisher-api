package com.illini.grades.controller;

import com.illini.grades.dto.CourseDetailDto;
import com.illini.grades.dto.CourseGradesResponseDto;
import com.illini.grades.dto.CourseSummaryDto;
import com.illini.grades.dto.PagedResponse;
import com.illini.grades.dto.ScheduledSectionDto;
import com.illini.grades.service.CourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/courses")
@Tag(name = "Courses")
public class CourseController {

    private final CourseService courseService;
    private final com.illini.grades.service.SectionScheduleIngestionService sectionScheduleIngestionService;

    public CourseController(CourseService courseService, com.illini.grades.service.SectionScheduleIngestionService sectionScheduleIngestionService) {
        this.courseService = courseService;
        this.sectionScheduleIngestionService = sectionScheduleIngestionService;
    }

    @GetMapping
    public PagedResponse<CourseSummaryDto> listCourses(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) Integer number,
            @RequestParam(required = false) List<String> sectionType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) List<String> cohort,
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "and") String filterMode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int perPage,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String order) {
        return courseService.listCourses(query, subject, instructor, number, sectionType, level, cohort, term, filterMode, page, perPage, sort, order);
    }

    @GetMapping("/{id}")
    public CourseDetailDto getCourse(@PathVariable Long id) {
        return courseService.getCourse(id);
    }

    @GetMapping("/{id}/grades")
    public CourseGradesResponseDto getCourseGrades(@PathVariable Long id) {
        return courseService.getCourseGrades(id);
    }

    @GetMapping("/{id}/scheduled-sections")
    public List<ScheduledSectionDto> getScheduledSections(@PathVariable Long id, @RequestParam String term) {
        return courseService.getScheduledSections(id, term);
    }

    @PostMapping("/{id}/sections/refresh")
    public com.illini.grades.service.SectionScheduleIngestionService.CourseRefreshStatus refreshSections(
            @PathVariable Long id,
            @RequestParam(required = false) String term) {
        return sectionScheduleIngestionService.refreshCourseSections(id, term);
    }

    @GetMapping("/{id}/sections/refresh-status")
    public com.illini.grades.service.SectionScheduleIngestionService.CourseRefreshStatus getRefreshStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String term) {
        return sectionScheduleIngestionService.getCourseRefreshStatus(id, term);
    }
}

