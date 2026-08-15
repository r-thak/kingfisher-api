ALTER TABLE course_offerings ADD COLUMN IF NOT EXISTS last_refreshed_at TIMESTAMP WITH TIME ZONE;

-- Seed existing offerings with scheduled sections with today's ingestion timestamp
UPDATE course_offerings 
SET last_refreshed_at = NOW() 
WHERE id IN (SELECT DISTINCT course_offering_id FROM scheduled_sections);
