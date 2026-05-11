ALTER TABLE contents
    ADD COLUMN external_id VARCHAR(50);

CREATE UNIQUE INDEX uk_contents_external
    ON contents (external_id, content_type)
    WHERE external_id IS NOT NULL;
