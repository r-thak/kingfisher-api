package com.illini.grades.service;

import com.illini.grades.dto.*;
import com.illini.grades.entity.CourseOffering;
import com.illini.grades.entity.Section;
import com.illini.grades.exception.ResourceNotFoundException;
import com.illini.grades.repository.CourseOfferingRepository;
import com.illini.grades.repository.SectionRepository;
import com.illini.grades.util.GpaCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final SectionRepository sectionRepository;
    private final String baseUrl;

    public CourseOfferingService(CourseOfferingRepository courseOfferingRepository, SectionRepository sectionRepository, @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.sectionRepository = sectionRepository;
        this.baseUrl = baseUrl;
    }

    public CourseOfferingDetailDto getCourseOffering(Long id) {
        CourseOffering co = courseOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        var course = co.getCourse();
        var subject = course.getSubject();
        var term = co.getTerm();

        SubjectSummaryDto subjectDto = new SubjectSummaryDto(subject.getId(), subject.getCode());
        CourseSummaryDto courseDto = new CourseSummaryDto(course.getId(), subjectDto, course.getNumber(), course.getTitle(), baseUrl + "/v1/courses/" + course.getId());
        TermDto termDto = new TermDto(term.getId(), term.getYear(), term.getSeason(), term.getYearTerm());

        List<Section> sections = sectionRepository.findByCourseOfferingId(co.getId());
        List<SectionGradeDto> sectionDtos = sections.stream()
                .map(s -> new SectionGradeDto(
                        s.getId(),
                        s.getSchedType(),
                        new InstructorSummaryDto(s.getInstructor().getId(), s.getInstructor().getName()),
                        GpaCalculator.fromCounts(
                                s.getAPlus(), s.getA(), s.getAMinus(),
                                s.getBPlus(), s.getB(), s.getBMinus(),
                                s.getCPlus(), s.getC(), s.getCMinus(),
                                s.getDPlus(), s.getD(), s.getDMinus(),
                                s.getF(), s.getW()
                        )
                ))
                .collect(Collectors.toList());

        return new CourseOfferingDetailDto(id, courseDto, termDto, sectionDtos);
    }
}
