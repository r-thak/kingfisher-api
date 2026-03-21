CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE terms (
    id BIGSERIAL PRIMARY KEY,
    year SMALLINT NOT NULL,
    season VARCHAR(10) NOT NULL,
    year_term VARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    number SMALLINT NOT NULL,
    title VARCHAR(255),
    UNIQUE (subject_id, number)
);

CREATE TABLE instructors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE course_offerings (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id),
    term_id BIGINT NOT NULL REFERENCES terms(id),
    UNIQUE (course_id, term_id)
);

CREATE TABLE sections (
    id BIGSERIAL PRIMARY KEY,
    course_offering_id BIGINT NOT NULL REFERENCES course_offerings(id),
    instructor_id BIGINT NOT NULL REFERENCES instructors(id),
    sched_type VARCHAR(10) NOT NULL,
    a_plus SMALLINT NOT NULL DEFAULT 0,
    a SMALLINT NOT NULL DEFAULT 0,
    a_minus SMALLINT NOT NULL DEFAULT 0,
    b_plus SMALLINT NOT NULL DEFAULT 0,
    b SMALLINT NOT NULL DEFAULT 0,
    b_minus SMALLINT NOT NULL DEFAULT 0,
    c_plus SMALLINT NOT NULL DEFAULT 0,
    c SMALLINT NOT NULL DEFAULT 0,
    c_minus SMALLINT NOT NULL DEFAULT 0,
    d_plus SMALLINT NOT NULL DEFAULT 0,
    d SMALLINT NOT NULL DEFAULT 0,
    d_minus SMALLINT NOT NULL DEFAULT 0,
    f SMALLINT NOT NULL DEFAULT 0,
    w SMALLINT NOT NULL DEFAULT 0,
    students SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (course_offering_id, instructor_id, sched_type)
);

CREATE TABLE course_grades (
    course_id BIGINT PRIMARY KEY REFERENCES courses(id),
    total_students INT NOT NULL,
    avg_students DOUBLE PRECISION NOT NULL,
    gpa DOUBLE PRECISION NOT NULL
);

CREATE TABLE subject_grades (
    subject_id BIGINT PRIMARY KEY REFERENCES subjects(id),
    total_students INT NOT NULL,
    avg_students DOUBLE PRECISION NOT NULL,
    gpa DOUBLE PRECISION NOT NULL
);

CREATE TABLE instructor_grades (
    instructor_id BIGINT PRIMARY KEY REFERENCES instructors(id),
    total_students INT NOT NULL,
    avg_students DOUBLE PRECISION NOT NULL,
    gpa DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_sections_offering ON sections(course_offering_id);
CREATE INDEX idx_sections_instructor ON sections(instructor_id);
CREATE INDEX idx_offerings_course ON course_offerings(course_id);
CREATE INDEX idx_offerings_term ON course_offerings(term_id);
CREATE INDEX idx_courses_subject ON courses(subject_id);
CREATE INDEX idx_courses_title_trgm ON courses USING GIN (title gin_trgm_ops);

ALTER DATABASE illini_grades SET pg_trgm.similarity_threshold = 0.1;