package com.illini.grades.service;

import com.illini.grades.dto.CourseSummaryDto;
import com.illini.grades.dto.SubjectDetailDto;
import com.illini.grades.dto.SubjectSummaryDto;
import com.illini.grades.entity.Course;
import com.illini.grades.entity.CourseGrade;
import com.illini.grades.entity.Subject;
import com.illini.grades.exception.ResourceNotFoundException;
import com.illini.grades.repository.CourseRepository;
import com.illini.grades.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final String baseUrl;

    public SubjectService(SubjectRepository subjectRepository, CourseRepository courseRepository, @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.baseUrl = baseUrl;
    }

    public List<SubjectSummaryDto> listSubjects() {
        return subjectRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(s -> new SubjectSummaryDto(s.getId(), s.getCode()))
                .collect(Collectors.toList());
    }

    public SubjectDetailDto getSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        List<CourseSummaryDto> courses = courseRepository.findAll((root, query, cb) -> cb.equal(root.get("subject"), subject))
                .stream()
                .map(c -> new CourseSummaryDto(
                        c.getId(),
                        new SubjectSummaryDto(subject.getId(), subject.getCode()),
                        c.getNumber(),
                        c.getTitle(),
                        c.getCourseGrade() != null ? c.getCourseGrade().getGpa() : null,
                        c.getCourseGrade() != null ? c.getCourseGrade().getTotalStudents() : null,
                        baseUrl + "/v1/courses/" + c.getId()
                ))
                .collect(Collectors.toList());

        return new SubjectDetailDto(subject.getId(), subject.getCode(), courses);
    }
}
