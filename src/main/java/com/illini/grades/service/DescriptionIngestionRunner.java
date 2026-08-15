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

/**
 * Loads course descriptions from a CSV (Subject, Number, Description) sourced from the
 * public UIUC Course Explorer (CIS) REST API. Mirrors PrerequisiteIngestionRunner, but
 * uses an UPDATE ... WHERE description IS NULL guard per-row instead of a table-level
 * count check, so re-running with a fuller/updated CSV backfills newly-fetched rows
 * without clobbering anything or requiring a full reset.
 */
@Component
public class DescriptionIngestionRunner implements ApplicationRunner {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final EntityManager entityManager;

    public DescriptionIngestionRunner(CourseRepository courseRepository, SubjectRepository subjectRepository, EntityManager entityManager) {
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        ClassPathResource resource = new ClassPathResource("uiuc-course-descriptions.csv");
        if (!resource.exists()) {
            System.err.println("uiuc-course-descriptions.csv not found in resources, skipping description ingestion.");
            return;
        }

        System.out.println("Starting course description ingestion...");

        int updated = 0;
        try (CSVParser parser = new CSVParser(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {

            for (CSVRecord record : parser) {
                try {
                    String subjectCode = record.get("Subject");
                    Short number = Short.parseShort(record.get("Number"));
                    String description = record.get("Description");
                    if (description == null || description.isBlank()) continue;

                    Optional<Subject> subjectOpt = subjectRepository.findByCode(subjectCode);
                    if (subjectOpt.isEmpty()) continue;

                    List<Course> courses = courseRepository.findBySubjectIdAndNumber(subjectOpt.get().getId(), number);
                    for (Course course : courses) {
                        int rows = entityManager.createNativeQuery(
                                "UPDATE courses SET description = ? WHERE id = ? AND description IS NULL")
                                .setParameter(1, description)
                                .setParameter(2, course.getId())
                                .executeUpdate();
                        updated += rows;
                    }

                    if (updated > 0 && updated % 500 == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                } catch (Exception ex) {
                    System.err.println("Skipping description row due to error: " + ex.getMessage());
                }
            }
        }

        System.out.println("Finished course description ingestion. Updated " + updated + " courses.");
    }
}
