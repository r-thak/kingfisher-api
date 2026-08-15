package com.illini.grades.service;

import com.illini.grades.entity.CourseOffering;
import com.illini.grades.entity.Instructor;
import com.illini.grades.entity.ScheduledSection;
import com.illini.grades.entity.ScheduledSectionMeeting;
import com.illini.grades.repository.InstructorRepository;
import com.illini.grades.repository.ScheduledSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies one parsed section (plus its meetings/instructors) to the database in a
 * single transaction. Split out from SectionScheduleIngestionService so @Transactional
 * actually takes effect (Spring proxies don't intercept same-class self-invocation).
 */
@Service
public class ScheduledSectionWriter {

    private final ScheduledSectionRepository scheduledSectionRepository;
    private final InstructorRepository instructorRepository;

    public ScheduledSectionWriter(ScheduledSectionRepository scheduledSectionRepository, InstructorRepository instructorRepository) {
        this.scheduledSectionRepository = scheduledSectionRepository;
        this.instructorRepository = instructorRepository;
    }

    @Transactional
    public void upsert(CourseOffering offering, String crn, SectionScheduleIngestionService.ParsedSection parsed) {
        ScheduledSection section = scheduledSectionRepository.findByCourseOfferingIdAndCrn(offering.getId(), crn)
                .orElseGet(ScheduledSection::new);
        section.setCourseOffering(offering);
        section.setCrn(crn);
        section.setSectionNumber(parsed.sectionNumber());
        section.setStatusCode(parsed.statusCode());
        section.setPartOfTerm(parsed.partOfTerm());
        section.setStartDate(parsed.startDate());
        section.setEndDate(parsed.endDate());
        section.setNotes(parsed.notes());

        section.getMeetings().clear();

        short idx = 0;
        for (SectionScheduleIngestionService.ParsedMeeting pm : parsed.meetings()) {
            ScheduledSectionMeeting meeting = new ScheduledSectionMeeting();
            meeting.setScheduledSection(section);
            meeting.setMeetingIndex(idx++);
            meeting.setStartTime(pm.startTime());
            meeting.setEndTime(pm.endTime());
            meeting.setDaysOfWeek(pm.daysOfWeek());
            meeting.setRoomNumber(pm.roomNumber());
            meeting.setBuildingName(pm.buildingName());
            meeting.setTypeCode(pm.typeCode());
            meeting.setTypeDescription(pm.typeDescription());

            for (String instructorName : pm.instructorNames()) {
                if (instructorName == null || instructorName.isBlank()) continue;
                Instructor instructor = instructorRepository.findByName(instructorName).orElseGet(() -> {
                    Instructor i = new Instructor();
                    i.setName(instructorName);
                    return instructorRepository.save(i);
                });
                meeting.getInstructors().add(instructor);
            }

            section.getMeetings().add(meeting);
        }

        scheduledSectionRepository.save(section);
    }
}
