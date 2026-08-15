-- Live/upcoming-term section schedules, sourced from the UIUC Course Explorer
-- schedule API. Distinct from `sections`, which is historical grade-distribution
-- data keyed by (course_offering, instructor, sched_type). A scheduled_section
-- is keyed by CRN and can have multiple meeting patterns (e.g. lecture + lab),
-- each with its own instructor list.

CREATE TABLE scheduled_sections (
    id BIGSERIAL PRIMARY KEY,
    course_offering_id BIGINT NOT NULL REFERENCES course_offerings(id) ON DELETE CASCADE,
    crn VARCHAR(20) NOT NULL,
    section_number VARCHAR(10),
    status_code VARCHAR(10),
    part_of_term VARCHAR(10),
    start_date DATE,
    end_date DATE,
    notes TEXT,
    UNIQUE (course_offering_id, crn)
);

CREATE INDEX idx_scheduled_sections_course_offering_id ON scheduled_sections(course_offering_id);

CREATE TABLE scheduled_section_meetings (
    id BIGSERIAL PRIMARY KEY,
    scheduled_section_id BIGINT NOT NULL REFERENCES scheduled_sections(id) ON DELETE CASCADE,
    meeting_index SMALLINT NOT NULL,
    start_time VARCHAR(10),
    end_time VARCHAR(10),
    days_of_week VARCHAR(10),
    room_number VARCHAR(50),
    building_name VARCHAR(255),
    type_code VARCHAR(10),
    type_description VARCHAR(100),
    UNIQUE (scheduled_section_id, meeting_index)
);

CREATE TABLE scheduled_section_meeting_instructors (
    meeting_id BIGINT NOT NULL REFERENCES scheduled_section_meetings(id) ON DELETE CASCADE,
    instructor_id BIGINT NOT NULL REFERENCES instructors(id),
    PRIMARY KEY (meeting_id, instructor_id)
);
