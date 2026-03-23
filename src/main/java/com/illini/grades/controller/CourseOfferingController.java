package com.illini.grades.controller;

import com.illini.grades.dto.CourseOfferingDetailDto;
import com.illini.grades.service.CourseOfferingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/course_offerings")
@Tag(name = "Course Offerings")
public class CourseOfferingController {

    private final CourseOfferingService courseOfferingService;

    public CourseOfferingController(CourseOfferingService courseOfferingService) {
        this.courseOfferingService = courseOfferingService;
    }

    @GetMapping("/{id}")
    public CourseOfferingDetailDto getCourseOffering(@PathVariable Long id) {
        return courseOfferingService.getCourseOffering(id);
    }
}
