package com.illini.grades.service;

import com.illini.grades.entity.*;
import com.illini.grades.repository.*;
import jakarta.persistence.EntityManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
public class IngestionService {

    private final TermRepository termRepository;
    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final SectionRepository sectionRepository;
    private final GradeAggregationService gradeAggregationService;
    private final EntityManager entityManager;

    public IngestionService(TermRepository termRepository, SubjectRepository subjectRepository, CourseRepository courseRepository, InstructorRepository instructorRepository, CourseOfferingRepository courseOfferingRepository, SectionRepository sectionRepository, GradeAggregationService gradeAggregationService, EntityManager entityManager) {
        this.termRepository = termRepository;
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.instructorRepository = instructorRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.sectionRepository = sectionRepository;
        this.gradeAggregationService = gradeAggregationService;
        this.entityManager = entityManager;
    }

    @Transactional
    public void ingest(InputStream csvInputStream) {
        try (CSVParser parser = new CSVParser(new InputStreamReader(csvInputStream, StandardCharsets.UTF_8), CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {
            int count = 0;
            for (CSVRecord record : parser) {
                try {
                    String yearStr = record.get("Year");
                    String termStr = record.get("Term");
                    String yearTermStr = record.get("YearTerm");
                    String subjectCode = record.get("Subject");
                    String numberStr = record.get("Number");
                    String courseTitle = record.get("Course Title");
                    String schedType = record.get("Sched Type");
                    String instructorName = record.get("Primary Instructor");

                    Term term = termRepository.findByYearTerm(yearTermStr).orElseGet(() -> {
                        Term t = new Term();
                        t.setYear(Short.parseShort(yearStr));
                        t.setSeason(termStr);
                        t.setYearTerm(yearTermStr);
                        return termRepository.save(t);
                    });

                    Subject subject = subjectRepository.findByCode(subjectCode).orElseGet(() -> {
                        Subject s = new Subject();
                        s.setCode(subjectCode);
                        return subjectRepository.save(s);
                    });

                    Short number = Short.parseShort(numberStr);
                    Course course = courseRepository.findBySubjectIdAndNumber(subject.getId(), number).orElseGet(() -> {
                        Course c = new Course();
                        c.setSubject(subject);
                        c.setNumber(number);
                        return c;
                    });
                    course.setTitle(courseTitle);
                    final Course savedCourse = courseRepository.save(course);

                    Instructor instructor = instructorRepository.findByName(instructorName).orElseGet(() -> {
                        Instructor i = new Instructor();
                        i.setName(instructorName);
                        return instructorRepository.save(i);
                    });

                    CourseOffering co = courseOfferingRepository.findByCourseIdAndTermId(savedCourse.getId(), term.getId()).orElseGet(() -> {
                        CourseOffering c = new CourseOffering();
                        c.setCourse(savedCourse);
                        c.setTerm(term);
                        return courseOfferingRepository.save(c);
                    });

                    Section section = sectionRepository.findByCourseOfferingIdAndInstructorIdAndSchedType(co.getId(), instructor.getId(), schedType).orElseGet(() -> {
                        Section s = new Section();
                        s.setCourseOffering(co);
                        s.setInstructor(instructor);
                        s.setSchedType(schedType);
                        return s;
                    });

                    section.setAPlus(parseShort(record.get("A+")));
                    section.setA(parseShort(record.get("A")));
                    section.setAMinus(parseShort(record.get("A-")));
                    section.setBPlus(parseShort(record.get("B+")));
                    section.setB(parseShort(record.get("B")));
                    section.setBMinus(parseShort(record.get("B-")));
                    section.setCPlus(parseShort(record.get("C+")));
                    section.setC(parseShort(record.get("C")));
                    section.setCMinus(parseShort(record.get("C-")));
                    section.setDPlus(parseShort(record.get("D+")));
                    section.setD(parseShort(record.get("D")));
                    section.setDMinus(parseShort(record.get("D-")));
                    section.setF(parseShort(record.get("F")));
                    section.setW(parseShort(record.get("W")));
                    section.setStudents(parseShort(record.get("Students")));

                    sectionRepository.save(section);

                    count++;
                    if (count % 500 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                } catch (Exception ex) {
                    System.err.println("Skipping row due to error: " + ex.getMessage());
                }
            }
            
            entityManager.flush();
            entityManager.clear();
            
            gradeAggregationService.recomputeAll();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV", e);
        }
    }

    private Short parseShort(String val) {
        if (val == null || val.isBlank()) return 0;
        try {
            return Short.parseShort(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
