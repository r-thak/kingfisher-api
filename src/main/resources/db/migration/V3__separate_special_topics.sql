ALTER TABLE courses DROP CONSTRAINT courses_subject_id_number_key;
ALTER TABLE courses ADD CONSTRAINT courses_subject_id_number_title_key UNIQUE (subject_id, number, title);
