CREATE TABLE course_prerequisites (
    course_id BIGINT NOT NULL,
    prerequisite_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, prerequisite_id),
    CONSTRAINT fk_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_prerequisite FOREIGN KEY (prerequisite_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE INDEX idx_course_prerequisites_prerequisite_id ON course_prerequisites (prerequisite_id);
