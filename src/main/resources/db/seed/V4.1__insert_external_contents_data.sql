-- 수동 등록 콘텐츠 (external_id = NULL)
INSERT INTO contents (title, description, content_type, avg_rating, tags, thumbnail_key)
SELECT
    'Content Title ' || i,
    'Description for content ' || i,
    CASE WHEN i % 3 = 0 THEN 'movie' WHEN i % 3 = 1 THEN 'tvSeries' ELSE 'sport' END,
    (i % 5) + 1.0,
    '["tag1", "tag2"]'::jsonb,
    'thumbnail_' || i || '.png'
FROM generate_series(1, 10) i;

-- TMDB 수집 콘텐츠 (영화)
INSERT INTO contents (title, description, content_type, avg_rating, tags, thumbnail_key, release_date, external_id)
VALUES
    ('Star Wars', 'A long time ago in a galaxy far, far away...', 'movie',
     0.0, '["SF", "모험"]'::jsonb, 'https://image.tmdb.org/t/p/w500/6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg',
     '1977-05-25'::timestamptz, '11'),

    ('The Dark Knight', 'Batman raises the stakes in his war on crime.', 'movie',
     0.0, '["액션", "범죄"]'::jsonb, 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911BTUgMe1nREA.jpg',
     '2008-07-18'::timestamptz, '155'),

    ('Inception', 'A thief who steals corporate secrets through dream-sharing technology.', 'movie',
     0.0, '["SF", "스릴러"]'::jsonb, 'https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg',
     '2010-07-16'::timestamptz, '27205');

-- TMDB 수집 콘텐츠 (TV 시리즈)
INSERT INTO contents (title, description, content_type, avg_rating, tags, thumbnail_key, release_date, external_id)
VALUES
    ('Breaking Bad', 'A chemistry teacher diagnosed with cancer turns to making meth.', 'tvSeries',
     0.0, '["범죄", "드라마"]'::jsonb, 'https://image.tmdb.org/t/p/w500/ggFHVNu6YYI5L9pCfOacjizRGt.jpg',
     '2008-01-20'::timestamptz, '1396'),

    ('Attack on Titan', 'Humanity fights for survival against giant humanoid Titans.', 'tvSeries',
     0.0, '["애니메이션", "액션"]'::jsonb, 'https://image.tmdb.org/t/p/w500/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg',
     '2013-04-07'::timestamptz, '1429');
