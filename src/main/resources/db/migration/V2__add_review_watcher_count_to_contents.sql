ALTER TABLE contents
    ADD COLUMN review_count  INT NOT NULL DEFAULT 0,
    ADD COLUMN watcher_count INT NOT NULL DEFAULT 0,

    ADD CONSTRAINT chk_contents_review_count_non_negative CHECK (review_count >= 0),
    ADD CONSTRAINT chk_contents_watcher_count_non_negative CHECK (watcher_count >= 0);