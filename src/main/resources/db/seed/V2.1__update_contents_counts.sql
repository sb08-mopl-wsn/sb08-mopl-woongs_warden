UPDATE contents SET
    review_count = (FLOOR(RANDOM() * 30))::int,
    watcher_count = (FLOOR(RANDOM() * 5))::int;