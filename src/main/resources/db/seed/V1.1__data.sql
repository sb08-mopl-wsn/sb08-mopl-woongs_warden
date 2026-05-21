-- 1. 유저 (users) 10명 생성
INSERT INTO users (name, email, password, role, is_locked, social_type, social_id)
SELECT
    'User ' || i,
    'user' || i || '@example.com',
    'password_hash_' || i,
    'USER',
    FALSE,
    CASE WHEN i % 2 = 0 THEN 'KAKAO' ELSE 'GOOGLE' END,
    'social_id_' || i
FROM generate_series(1, 10) i;

-- 2. 콘텐츠 (contents) 10개 생성
INSERT INTO contents (title, description, content_type, avg_rating, tags, thumbnail_key)
SELECT
    'Content Title ' || i,
    'Description for content ' || i,
    CASE WHEN i % 3 = 0 THEN 'movie' WHEN i % 3 = 1 THEN 'tvSeries' ELSE 'sport' END,
    (i % 5) + 1.0,
    '["tag1", "tag2"]'::jsonb,
    'thumbnail_' || i || '.png'
FROM generate_series(1, 10) i;

-- 3. 리뷰 (reviews) 10개 생성 (유저 N이 콘텐츠 N에 리뷰 작성)
INSERT INTO reviews (rating, description, user_id, content_id)
SELECT
    (u.rn % 5) + 1.0,
    'This is a review ' || u.rn,
    u.id,
    c.id
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u
         JOIN (SELECT id, ROW_NUMBER() OVER () as rn FROM contents LIMIT 10) c ON u.rn = c.rn;

-- 4. 플레이리스트 (playlists) 10개 생성
INSERT INTO playlists (title, description, subscriber_count, content_count, user_id)
SELECT
    'Playlist ' || u.rn,
    'My awesome playlist ' || u.rn,
    u.rn * 10,
    u.rn * 5,
    u.id
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u;

-- 5. 플레이리스트 콘텐츠 (playlist_contents) 10개 연결 (플레이리스트 N에 콘텐츠 N 추가)
INSERT INTO playlist_contents (content_id, playlist_id)
SELECT c.id, p.id
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM contents LIMIT 10) c
         JOIN (SELECT id, ROW_NUMBER() OVER () as rn FROM playlists LIMIT 10) p ON c.rn = p.rn;

-- 6. 플레이리스트 구독 내역 (playlist_subscriptions) 10개 (유저 N이 플레이리스트 11-N 구독)
-- 주의: id 기본값이 없으므로 gen_random_uuid()를 직접 주입
INSERT INTO playlist_subscriptions (id, user_id, playlist_id)
SELECT gen_random_uuid(), u.id, p.id
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u
         JOIN (SELECT id, ROW_NUMBER() OVER () as rn FROM playlists LIMIT 10) p ON (11 - u.rn) = p.rn;

-- 7. 알림 (notifications) 10개 생성
INSERT INTO notifications (user_id, title, content, level)
SELECT
    u.id,
    'Notification ' || u.rn,
    'You have a new alert.',
    CASE WHEN u.rn % 3 = 0 THEN 'ERROR' WHEN u.rn % 3 = 1 THEN 'WARNING' ELSE 'INFO' END
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u;

-- 8. 팔로우 (follows) 10개 (유저 1->2, 2->3, ..., 10->1 팔로우)
INSERT INTO follows (follower_id, followee_id)
SELECT u1.id, u2.id
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u1
         JOIN (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u2
              ON u2.rn = CASE WHEN u1.rn = 10 THEN 1 ELSE u1.rn + 1 END;

-- 9. 대화방 (conversations) 10개 (유저 1&2, 2&3, ..., 10&1 대화방)
INSERT INTO conversations (sender_id, receiver_id)
SELECT u1.id, u2.id
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u1
         JOIN (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u2
              ON u2.rn = CASE WHEN u1.rn = 10 THEN 1 ELSE u1.rn + 1 END;

-- 10. DM (direct_messages) 10개 (생성된 대화방마다 1개씩 메시지 전송)
INSERT INTO direct_messages (conversation_id, sender_id, content)
SELECT id, sender_id, 'Hello! This is a test message.'
FROM conversations LIMIT 10;

-- 11. JWT (jwt) 10개 생성
-- 주의: id 기본값이 없으므로 gen_random_uuid()를 직접 주입
INSERT INTO jwt (id, user_id, created_at, expires_at, refresh_token)
SELECT
    gen_random_uuid(),
    u.id,
    NOW(),
    NOW() + INTERVAL '14 days',
    'refresh_token_string_' || u.rn
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u;

-- 12. 시청 세션 (watching_sessions) 10개 (유저 N이 콘텐츠 11-N 시청)
INSERT INTO watching_sessions (content_id, user_id)
SELECT c.id, u.id
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM contents LIMIT 10) c
         JOIN (SELECT id, ROW_NUMBER() OVER () as rn FROM users LIMIT 10) u
              ON (11 - c.rn) = u.rn;