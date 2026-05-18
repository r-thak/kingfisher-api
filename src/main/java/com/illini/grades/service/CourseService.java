package com.illini.grades.service;

import org.springframework.transaction.annotation.Transactional;

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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageImpl;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final SectionRepository sectionRepository;
    private final String baseUrl;

    @PersistenceContext
    private EntityManager em;

    public CourseService(CourseRepository courseRepository, CourseOfferingRepository courseOfferingRepository, SectionRepository sectionRepository, @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.courseRepository = courseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.sectionRepository = sectionRepository;
        this.baseUrl = baseUrl;
    }

    public PagedResponse<CourseSummaryDto> listCourses(String query, String subjectCode, String instructorName, Integer number, int page, int perPage, String sort, String order) {
        if (perPage > 100) perPage = 100;
        
        Page<Course> result;
        if (query != null && !query.isBlank()) {
            String q = normalizeQuery(query);

            boolean isExactMatch = q.matches("^[a-z]+\\s+\\d+$");
            
            StringBuilder sql = new StringBuilder();
            if (isExactMatch && (sort == null || "match".equalsIgnoreCase(sort) || "".equals(sort))) {
                sql.append("""
                    WITH RECURSIVE
                      target_course AS (
                          SELECT c.id
                          FROM courses c JOIN subjects s ON s.id = c.subject_id
                          WHERE LOWER(s.code || ' ' || c.number::text) = :query LIMIT 1
                      ),
                      forward_chain(id, depth) AS (
                          SELECT id, 0 FROM target_course
                          UNION ALL
                          SELECT cp.course_id, fc.depth + 1
                          FROM course_prerequisites cp
                          JOIN forward_chain fc ON cp.prerequisite_id = fc.id
                          WHERE fc.depth < 5
                      ),
                      backward_chain(id, depth) AS (
                          SELECT id, 0 FROM target_course
                          UNION ALL
                          SELECT cp.prerequisite_id, bc.depth + 1
                          FROM course_prerequisites cp
                          JOIN backward_chain bc ON cp.course_id = bc.id
                          WHERE bc.depth < 5
                      ),
                      chain_distances AS (
                          SELECT id, MIN(depth) as depth, MAX(is_after) as is_after FROM (
                              SELECT id, MIN(depth) as depth, 1 as is_after FROM forward_chain WHERE depth > 0 GROUP BY id
                              UNION ALL
                              SELECT id, MIN(depth) as depth, 0 as is_after FROM backward_chain WHERE depth > 0 GROUP BY id
                          ) sub GROUP BY id
                      )
                    """);
            }
            
            sql.append("""
                SELECT c.* FROM courses c
                JOIN subjects s ON s.id = c.subject_id
                LEFT JOIN course_grades cg ON c.id = cg.course_id
            """);
            
            if (isExactMatch && (sort == null || "match".equalsIgnoreCase(sort) || "".equals(sort))) {
                sql.append(" LEFT JOIN chain_distances cd ON c.id = cd.id ");
            }
            
            sql.append("""
                 WHERE (c.title % :query
                    OR (s.code || ' ' || c.number::text) % :query
                    OR (s.code || c.number::text) % :query
                    OR LOWER(c.title) LIKE '%' || :query || '%'
                    OR LOWER(s.code || ' ' || c.number::text) LIKE '%' || :query || '%')
            """);

            if (subjectCode != null && !subjectCode.isBlank()) {
                sql.append(" AND s.code = :subject ");
            }

            if (number != null) {
                sql.append(" AND c.number = :number ");
            }

            String[] instructorTokens = null;
            if (instructorName != null && !instructorName.isBlank()) {
                instructorTokens = Arrays.stream(instructorName.toLowerCase().split("\\s+"))
                                         .filter(t -> !t.isBlank())
                                         .toArray(String[]::new);
                if (instructorTokens.length > 0) {
                    sql.append(" AND EXISTS (SELECT 1 FROM course_offerings co JOIN sections sec ON sec.course_offering_id = co.id JOIN instructors i ON i.id = sec.instructor_id WHERE co.course_id = c.id ");
                    for (int i = 0; i < instructorTokens.length; i++) {
                        sql.append(" AND LOWER(i.name) LIKE :inst").append(i);
                    }
                    sql.append(") ");
                }
            }

            // Sorting
            String orderBy;
            if ("gpa".equalsIgnoreCase(sort)) {
                orderBy = "cg.gpa " + ("asc".equalsIgnoreCase(order) ? "ASC" : "DESC") + " NULLS LAST";
            } else if ("total_grades".equalsIgnoreCase(sort)) {
                orderBy = "cg.total_students " + ("asc".equalsIgnoreCase(order) ? "ASC" : "DESC") + " NULLS LAST";
            } else if ("title".equalsIgnoreCase(sort) || "name".equalsIgnoreCase(sort)) {
                orderBy = "c.title " + ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
            } else if ("avg_students".equalsIgnoreCase(sort) || "popularity".equalsIgnoreCase(sort)) {
                orderBy = "cg.avg_students " + ("asc".equalsIgnoreCase(order) ? "ASC" : "DESC") + " NULLS LAST";
            } else { // default to closest match
                if (isExactMatch) {
                    orderBy = "(CASE WHEN LOWER(s.code || ' ' || c.number::text) = :query THEN 1 ELSE 0 END) DESC, " +
                              "cd.depth ASC NULLS LAST, " +
                              "cd.is_after DESC NULLS LAST, " +
                              "(CASE WHEN :query ~ ('\\y' || LOWER(s.code) || '\\y') THEN 1.0 ELSE 0.0 END) + " +
                              "GREATEST(similarity(c.title, :query), similarity(s.code || ' ' || c.number::text, :query)) DESC NULLS LAST, " +
                              "cg.total_students DESC NULLS LAST";
                } else {
                    orderBy = "(CASE WHEN :query ~ ('\\y' || LOWER(s.code) || '\\y') THEN 1.0 ELSE 0.0 END) + " +
                              "GREATEST(similarity(c.title, :query), similarity(s.code || ' ' || c.number::text, :query)) DESC NULLS LAST, " +
                              "cg.total_students DESC NULLS LAST";
                }
            }

            String countSql = "SELECT count(*) FROM (" + sql.toString() + ") AS sq";
            sql.append(" ORDER BY ").append(orderBy);

            jakarta.persistence.Query queryObj = em.createNativeQuery(sql.toString(), Course.class);
            jakarta.persistence.Query countQueryObj = em.createNativeQuery(countSql);

            queryObj.setParameter("query", q);
            countQueryObj.setParameter("query", q);

            if (subjectCode != null && !subjectCode.isBlank()) {
                queryObj.setParameter("subject", subjectCode);
                countQueryObj.setParameter("subject", subjectCode);
            }

            if (number != null) {
                queryObj.setParameter("number", number);
                countQueryObj.setParameter("number", number);
            }

            if (instructorTokens != null && instructorTokens.length > 0) {
                for (int i = 0; i < instructorTokens.length; i++) {
                    queryObj.setParameter("inst" + i, "%" + instructorTokens[i] + "%");
                    countQueryObj.setParameter("inst" + i, "%" + instructorTokens[i] + "%");
                }
            }

            queryObj.setFirstResult((page - 1) * perPage);
            queryObj.setMaxResults(perPage);

            @SuppressWarnings("unchecked")
            List<Course> list = queryObj.getResultList();
            long total = ((Number) countQueryObj.getSingleResult()).longValue();
            result = new PageImpl<>(list, PageRequest.of(page - 1, perPage), total);
        } else {
            Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortBy;
            if ("gpa".equalsIgnoreCase(sort)) {
                sortBy = "courseGrade.gpa";
            } else if ("total_grades".equalsIgnoreCase(sort)) {
                sortBy = "courseGrade.totalStudents";
            } else if ("title".equalsIgnoreCase(sort) || "name".equalsIgnoreCase(sort)) {
                sortBy = "title";
            } else if ("avg_students".equalsIgnoreCase(sort) || "popularity".equalsIgnoreCase(sort)) {
                sortBy = "courseGrade.avgStudents";
            } else {
                sortBy = "courseGrade.avgStudents"; 
                if (sort == null || sort.isBlank() || "match".equalsIgnoreCase(sort)) {
                    direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
                    if ("match".equalsIgnoreCase(sort)) direction = Sort.Direction.DESC; // popularity fallback is DESC
                }
            }
            
            PageRequest pageRequest = PageRequest.of(page - 1, perPage, Sort.by(direction, sortBy));
            
            final String finalSortBy = sortBy;
            Specification<Course> spec = (root, q, cb) -> {
                if (q.getResultType() != Long.class && q.getResultType() != long.class) {
                    if ("courseGrade.gpa".equals(finalSortBy) || "courseGrade.totalStudents".equals(finalSortBy) || "courseGrade.avgStudents".equals(finalSortBy)) {
                        root.fetch("courseGrade", JoinType.LEFT);
                    }
                }
                var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                if (subjectCode != null && !subjectCode.isBlank()) {
                    predicates.add(cb.equal(root.get("subject").get("code"), subjectCode));
                }
                if (number != null) {
                    predicates.add(cb.equal(root.get("number"), number));
                }
                if (instructorName != null && !instructorName.isBlank()) {
                    String[] tokens = instructorName.toLowerCase().split("\\s+");
                    var subquery = q.subquery(Long.class);
                    var sRoot = subquery.from(Section.class);
                    var instructorJoin = sRoot.join("instructor");
                    
                    var tokenPredicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
                    for (String token : tokens) {
                        if (!token.isBlank()) {
                            tokenPredicates.add(cb.like(cb.lower(instructorJoin.get("name")), "%" + token + "%"));
                        }
                    }
                    
                    subquery.select(sRoot.get("courseOffering").get("course").get("id"))
                            .where(cb.and(tokenPredicates.toArray(new jakarta.persistence.criteria.Predicate[0])));
                    predicates.add(root.get("id").in(subquery));
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
                c.getCourseGrade() != null ? c.getCourseGrade().getGpa() : null,
                c.getCourseGrade() != null ? c.getCourseGrade().getTotalStudents() : null,
                baseUrl + "/v1/courses/" + c.getId()
        )).toList();

        return new PagedResponse<>(page, result.getTotalPages(), result.getTotalElements(), dtos);
    }

    private static final Map<String, String> SEARCH_SYNONYMS = new LinkedHashMap<>();
    static {
        // Subjects
        SEARCH_SYNONYMS.put("computer science", "CS");
        SEARCH_SYNONYMS.put("comp sci", "CS");
        SEARCH_SYNONYMS.put("math", "MATH");
        SEARCH_SYNONYMS.put("mathematics", "MATH");
        SEARCH_SYNONYMS.put("stat", "STAT");
        SEARCH_SYNONYMS.put("statistics", "STAT");
        SEARCH_SYNONYMS.put("phys", "PHYS");
        SEARCH_SYNONYMS.put("physics", "PHYS");
        SEARCH_SYNONYMS.put("econ", "ECON");
        SEARCH_SYNONYMS.put("economics", "ECON");
        SEARCH_SYNONYMS.put("bio", "IB");
        SEARCH_SYNONYMS.put("biology", "IB");
        SEARCH_SYNONYMS.put("chem", "CHEM");
        SEARCH_SYNONYMS.put("chemistry", "CHEM");
        
        // Course specific tricky terms
        SEARCH_SYNONYMS.put("algos", "algs");
        SEARCH_SYNONYMS.put("algorithms", "algs");
        SEARCH_SYNONYMS.put("introduction", "intro");
        SEARCH_SYNONYMS.put("computer", "comp");
        SEARCH_SYNONYMS.put("computation", "comp");
        SEARCH_SYNONYMS.put("computing", "comp");
        SEARCH_SYNONYMS.put("and", "&");
    }

    private String normalizeQuery(String q) {
        String normalized = q.toLowerCase().trim();
        
        for (Map.Entry<String, String> entry : SEARCH_SYNONYMS.entrySet()) {
            // Use word boundaries to avoid replacing parts of words (e.g., "statistical" -> "STATistical")
            normalized = normalized.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue().toLowerCase());
        }
        
        // Ensure space between subject and number (e.g., "cs374" -> "cs 374")
        normalized = normalized.replaceAll("([a-z])(\\d)", "$1 $2");
        
        // Handle common course renumberings/equivalents
        normalized = normalized.replaceAll("\\bcs\\s+241\\b", "cs 341");
        normalized = normalized.replaceAll("\\bcs\\s+125\\b", "cs 124");
        normalized = normalized.replaceAll("\\bcs\\s+242\\b", "cs 222");
        
        // Translate Arabic numerals to Roman numerals for course titles (e.g. "Calculus 3" -> "Calculus III")
        normalized = normalized.replaceAll("\\b1\\b", "i");
        normalized = normalized.replaceAll("\\b2\\b", "ii");
        normalized = normalized.replaceAll("\\b3\\b", "iii");
        normalized = normalized.replaceAll("\\b4\\b", "iv");
        normalized = normalized.replaceAll("\\b5\\b", "v");
        
        return normalized.trim();
    }

    public CourseDetailDto getCourse(Long id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(id);
        
        List<CourseOfferingSummaryDto> coDtos = offerings.stream().map(co -> new CourseOfferingSummaryDto(
                co.getId(),
                new TermDto(co.getTerm().getId(), co.getTerm().getYear(), co.getTerm().getSeason(), co.getTerm().getYearTerm()),
                baseUrl + "/v1/course_offerings/" + co.getId()
        )).toList();

        List<Course> crosslisted = courseRepository.findByTitleAndNumber(course.getTitle(), (short) course.getNumber());
        List<CourseSummaryDto> crosslistedDtos = crosslisted.stream()
                .filter(c -> !c.getId().equals(id))
                .map(c -> new CourseSummaryDto(
                        c.getId(),
                        new SubjectSummaryDto(c.getSubject().getId(), c.getSubject().getCode()),
                        c.getNumber(),
                        c.getTitle(),
                        c.getCourseGrade() != null ? c.getCourseGrade().getGpa() : null,
                        c.getCourseGrade() != null ? c.getCourseGrade().getTotalStudents() : null,
                        baseUrl + "/v1/courses/" + c.getId()
                )).toList();

        return new CourseDetailDto(
                id,
                new SubjectSummaryDto(course.getSubject().getId(), course.getSubject().getCode()),
                course.getNumber(),
                course.getTitle(),
                baseUrl + "/v1/courses/" + id + "/grades",
                coDtos,
                crosslistedDtos
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
