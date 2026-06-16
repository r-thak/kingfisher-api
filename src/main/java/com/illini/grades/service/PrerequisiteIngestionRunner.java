package com.illini.grades.service;

import com.illini.grades.entity.Course;
import com.illini.grades.entity.Subject;
import com.illini.grades.repository.CourseRepository;
import com.illini.grades.repository.SubjectRepository;
import jakarta.persistence.EntityManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
public class PrerequisiteIngestionRunner implements ApplicationRunner {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final EntityManager entityManager;

    public PrerequisiteIngestionRunner(CourseRepository courseRepository, SubjectRepository subjectRepository, EntityManager entityManager) {
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // Check if prerequisites are already loaded
        Number count = (Number) entityManager.createNativeQuery("SELECT count(*) FROM course_prerequisites").getSingleResult();
        if (count.longValue() > 0) {
            return;
        }

        System.out.println("Starting prerequisite ingestion...");

        ClassPathResource resource = new ClassPathResource("uiuc-prerequisites.csv");
        if (!resource.exists()) {
            System.err.println("uiuc-prerequisites.csv not found in resources!");
            return;
        }

        try (CSVParser parser = new CSVParser(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8), 
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {
            
            int inserted = 0;
            for (CSVRecord record : parser) {
                try {
                    String sourceCourseStr = record.get("Course"); // e.g., "CS 225"
                    String[] sourceParts = sourceCourseStr.split(" ");
                    if (sourceParts.length != 2) continue;
                    
                    String sourceSubjectCode = sourceParts[0];
                    Short sourceNumber = Short.parseShort(sourceParts[1]);

                    Optional<Subject> subjectOpt = subjectRepository.findByCode(sourceSubjectCode);
                    if (subjectOpt.isEmpty()) continue;
                    
                    List<Course> sourceCourses = courseRepository.findBySubjectIdAndNumber(subjectOpt.get().getId(), sourceNumber);
                    if (sourceCourses.isEmpty()) continue;

                    int prereqCount = Integer.parseInt(record.get("PrerequisiteNumber"));
                    for (int i = 0; i < prereqCount; i++) {
                        String prereqStr = record.get(String.valueOf(i)); // e.g., "CS 125"
                        if (prereqStr == null || prereqStr.isBlank()) continue;
                        
                        String[] prereqParts = prereqStr.split(" ");
                        if (prereqParts.length != 2) continue;

                        String prereqSubjectCode = prereqParts[0];
                        Short prereqNumber;
                        try {
                            prereqNumber = Short.parseShort(prereqParts[1]);
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        Optional<Subject> prereqSubjectOpt = subjectRepository.findByCode(prereqSubjectCode);
                        if (prereqSubjectOpt.isEmpty()) continue;

                        List<Course> prereqCourses = courseRepository.findBySubjectIdAndNumber(prereqSubjectOpt.get().getId(), prereqNumber);
                        if (prereqCourses.isEmpty()) continue;
                        
                        for (Course sourceCourse : sourceCourses) {
                            for (Course prereqCourse : prereqCourses) {
                                entityManager.createNativeQuery("INSERT INTO course_prerequisites (course_id, prerequisite_id) VALUES (?, ?) ON CONFLICT DO NOTHING")
                                        .setParameter(1, sourceCourse.getId())
                                        .setParameter(2, prereqCourse.getId())
                                        .executeUpdate();
                                inserted++;
                            }
                        }
                    }
                    
                    if (inserted % 500 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                } catch (Exception ex) {
                    System.err.println("Skipping prerequisite row due to error: " + ex.getMessage());
                }
            }
            System.out.println("Finished prerequisite ingestion. Inserted " + inserted + " records.");
        }
    }
}
