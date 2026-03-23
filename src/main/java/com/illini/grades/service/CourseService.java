package com.illini.grades.service;

import com.illini.grades.dto.*;
import com.illini.grades.entity.Course;
import com.illini.grades.entity.CourseOffering;
import com.illini.grades.entity.Section;
import com.illini.grades.exception.ResourceNotFoundException;
import com.illini.grades.repository.CourseOfferingRepository;
import com.illini.grades.repository.CourseRepository;
import com.illini.grades.repository.SectionRepository;
import com.illini.grades.util.GpaCalculator;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final SectionRepository sectionRepository;
    private final String baseUrl;

    public CourseService(CourseRepository courseRepository, CourseOfferingRepository courseOfferingRepository, SectionRepository sectionRepository, @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.courseRepository = courseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.sectionRepository = sectionRepository;
        this.baseUrl = baseUrl;
    }

    public PagedResponse<CourseSummaryDto> listCourses(String query, String subjectCode, Long instructorId, Integer number, int page, int perPage, String sort, String order) {
        if (perPage > 100) perPage = 100;
        
        Page<Course> result;
        if (query != null && !query.isBlank() && sort == null) {
            String q = normalizeQuery(query);
            result = courseRepository.searchByQuery(q, PageRequest.of(page - 1, perPage));
        } else {
            Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
            String sortBy = "title".equalsIgnoreCase(sort) || "name".equalsIgnoreCase(sort) ? "title" : "number";
            PageRequest pageRequest = PageRequest.of(page - 1, perPage, Sort.by(direction, sortBy));
            
            Specification<Course> spec = (root, q, cb) -> {
                var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                if (subjectCode != null && !subjectCode.isBlank()) {
                    predicates.add(cb.equal(root.get("subject").get("code"), subjectCode));
                }
                if (number != null) {
                    predicates.add(cb.equal(root.get("number"), number));
                }
                if (instructorId != null) {
                    var subquery = q.subquery(Long.class);
                    var sRoot = subquery.from(Section.class);
                    subquery.select(sRoot.get("courseOffering").get("course").get("id"))
                            .where(cb.equal(sRoot.get("instructor").get("id"), instructorId));
                    predicates.add(root.get("id").in(subquery));
                }
                if (query != null && !query.isBlank()) {
                    String qstr = query.toLowerCase();
                    predicates.add(cb.like(cb.lower(root.get("title")), "%" + qstr + "%"));
                }
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            };
            result = courseRepository.findAll(spec, pageRequest);
        }

        List<CourseSummaryDto> dtos = result.stream().map(c -> new CourseSummaryDto(
                c.getId(),
                new SubjectSummaryDto(c.getSubject().getId(), c.getSubject().getCode()),
                c.getNumber(),
                c.getTitle(),
                baseUrl + "/v1/courses/" + c.getId()
        )).toList();

        return new PagedResponse<>(page, result.getTotalPages(), result.getTotalElements(), dtos);
    }

    private String normalizeQuery(String q) {
        return q.toLowerCase().replaceAll("([a-z])(\\d)", "$1 $2").trim();
    }

    public CourseDetailDto getCourse(Long id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(id);
        
        List<CourseOfferingSummaryDto> coDtos = offerings.stream().map(co -> new CourseOfferingSummaryDto(
                co.getId(),
                new TermDto(co.getTerm().getId(), co.getTerm().getYear(), co.getTerm().getSeason(), co.getTerm().getYearTerm()),
                baseUrl + "/v1/course_offerings/" + co.getId()
        )).toList();

        return new CourseDetailDto(
                id,
                new SubjectSummaryDto(course.getSubject().getId(), course.getSubject().getCode()),
                course.getNumber(),
                course.getTitle(),
                baseUrl + "/v1/courses/" + id + "/grades",
                coDtos
        );
    }

    public CourseGradesResponseDto getCourseGrades(Long id) {
        if (!courseRepository.existsById(id)) throw new ResourceNotFoundException("Course not found");
        
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(id);
        List<Long> coIds = offerings.stream().map(CourseOffering::getId).toList();
        List<Section> allSections = sectionRepository.findByCourseOfferingIdIn(coIds);

        Map<Long, List<Section>> sectionsByCo = allSections.stream().collect(Collectors.groupingBy(s -> s.getCourseOffering().getId()));

        List<CourseOfferingGradeDto> coGradeDtos = new ArrayList<>();
        List<GradeDistributionDto> overallDists = new ArrayList<>();

        offerings.sort((o1, o2) -> {
            int y1 = o1.getTerm().getYear();
            int y2 = o2.getTerm().getYear();
            if (y1 != y2) return Integer.compare(y2, y1);
            return Integer.compare(seasonOrder(o2.getTerm().getSeason()), seasonOrder(o1.getTerm().getSeason()));
        });

        for (CourseOffering co : offerings) {
            List<Section> sections = sectionsByCo.getOrDefault(co.getId(), Collections.emptyList());
            List<SectionGradeDto> sDtos = new ArrayList<>();
            List<GradeDistributionDto> coDists = new ArrayList<>();

            for (Section s : sections) {
                GradeDistributionDto sd = GpaCalculator.fromCounts(
                        s.getAPlus(), s.getA(), s.getAMinus(),
                        s.getBPlus(), s.getB(), s.getBMinus(),
                        s.getCPlus(), s.getC(), s.getCMinus(),
                        s.getDPlus(), s.getD(), s.getDMinus(),
                        s.getF(), s.getW()
                );
                sDtos.add(new SectionGradeDto(
                        s.getId(),
                        s.getSchedType(),
                        new InstructorSummaryDto(s.getInstructor().getId(), s.getInstructor().getName()),
                        sd
                ));
                coDists.add(sd);
            }

            GradeDistributionDto coCum = GpaCalculator.sum(coDists);
            overallDists.add(coCum);

            coGradeDtos.add(new CourseOfferingGradeDto(
                    co.getTerm().getId(),
                    co.getTerm().getYearTerm(),
                    coCum,
                    sDtos
            ));
        }

        GradeDistributionDto cumulative = GpaCalculator.sum(overallDists);
        return new CourseGradesResponseDto(id, cumulative, coGradeDtos);
    }

    private int seasonOrder(String season) {
        switch (season.toLowerCase()) {
            case "spring": return 1;
            case "summer": return 2;
            case "fall": return 3;
            case "winter": return 4;
            default: return 0;
        }
    }
}
