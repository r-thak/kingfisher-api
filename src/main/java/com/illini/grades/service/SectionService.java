package com.illini.grades.service;

import com.illini.grades.dto.*;
import com.illini.grades.entity.Section;
import com.illini.grades.exception.ResourceNotFoundException;
import com.illini.grades.repository.SectionRepository;
import com.illini.grades.util.GpaCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final String baseUrl;

    public SectionService(SectionRepository sectionRepository, @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.sectionRepository = sectionRepository;
        this.baseUrl = baseUrl;
    }

    public SectionDetailDto getSection(Long id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        var co = section.getCourseOffering();
        var course = co.getCourse();
        var subject = course.getSubject();
        var term = co.getTerm();
        var instr = section.getInstructor();

        SubjectSummaryDto subjectDto = new SubjectSummaryDto(subject.getId(), subject.getCode());
        CourseSummaryDto courseDto = new CourseSummaryDto(course.getId(), subjectDto, course.getNumber(), course.getTitle(), baseUrl + "/v1/courses/" + course.getId());
        TermDto termDto = new TermDto(term.getId(), term.getYear(), term.getSeason(), term.getYearTerm());
        CourseOfferingSummaryDto coDto = new CourseOfferingSummaryDto(co.getId(), termDto, baseUrl + "/v1/course_offerings/" + co.getId());
        InstructorSummaryDto instrDto = new InstructorSummaryDto(instr.getId(), instr.getName());
        GradeDistributionDto grades = GpaCalculator.fromCounts(
                section.getAPlus(), section.getA(), section.getAMinus(),
                section.getBPlus(), section.getB(), section.getBMinus(),
                section.getCPlus(), section.getC(), section.getCMinus(),
                section.getDPlus(), section.getD(), section.getDMinus(),
                section.getF(), section.getW()
        );

        return new SectionDetailDto(id, coDto, courseDto, instrDto, section.getSchedType(), grades);
    }
}
