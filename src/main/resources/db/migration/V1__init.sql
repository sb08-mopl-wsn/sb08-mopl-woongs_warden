-- 유저 (users)
CREATE TABLE IF NOT EXISTS users
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    name                        VARCHAR(50)                 NOT NULL,
    email                       VARCHAR(100)                NOT NULL            UNIQUE,
    password                    VARCHAR(255)                    NULL,
    role                        VARCHAR(10)                 NOT NULL            DEFAULT 'USER',
    is_locked                   boolean                     NOT NULL            DEFAULT FALSE,
    profile_image_key           VARCHAR(255)                    NULL,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    social_type                 VARCHAR(8)                      NULL,
    social_id                   VARCHAR(100)                    NULL,

    CONSTRAINT ck_user_role   CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_social_type CHECK (social_type IN ('KAKAO', 'GOOGLE'))
);

-- 컨텐츠 (contents)
CREATE TABLE IF NOT EXISTS contents
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    title                       VARCHAR(100)                NOT NULL,
    description                 TEXT                        NOT NULL,
    content_type                VARCHAR(50)                 NOT NULL,
    avg_rating                  DECIMAL(2, 1)               NOT NULL            DEFAULT 0.0,
    tags                        JSONB                           NULL,
    thumbnail_key               VARCHAR(255)                NOT NULL,
    release_date                TIMESTAMPTZ                     NULL,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),

    CONSTRAINT ck_contents_content_type CHECK (content_type IN ('movie', 'tvSeries', 'sport'))
);

-- 리뷰 (reviews)
CREATE TABLE IF NOT EXISTS reviews
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    rating                      DOUBLE PRECISION            NOT NULL            CHECK (rating BETWEEN 0 AND 5),
    description                 TEXT                        NOT NULL,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    user_id                     UUID                        NOT NULL,
    content_id                  UUID                        NOT NULL,

    CONSTRAINT uk_reviews_user_content UNIQUE (user_id, content_id),

    CONSTRAINT fk_reviews_user_id    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_reviews_content_id FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE
);

-- 플레이리스트 (playlists)
CREATE TABLE IF NOT EXISTS playlists
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    title                       VARCHAR(100)                NOT NULL,
    description                 TEXT                        NOT NULL,
    subscriber_count            BIGINT                      NOT NULL            DEFAULT 0,
    content_count               BIGINT                      NOT NULL            DEFAULT 0,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    user_id                     UUID                        NOT NULL,

    CONSTRAINT fk_playlists_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 플레이리스트 콘텐츠 (playlist_contents)
CREATE TABLE IF NOT EXISTS playlist_contents
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    content_id                  UUID                        NOT NULL,
    playlist_id                 UUID                        NOT NULL,

    CONSTRAINT uk_playlist_contents UNIQUE (playlist_id, content_id),

    CONSTRAINT fk_playlist_contents_content_id  FOREIGN KEY (content_id)    REFERENCES contents     (id) ON DELETE CASCADE,
    CONSTRAINT fk_playlist_contents_playlist_id FOREIGN KEY (playlist_id)   REFERENCES playlists    (id) ON DELETE CASCADE
);

-- 플레이리스트 구독 내역
CREATE TABLE IF NOT EXISTS playlist_subscriptions
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    user_id                     UUID                        NOT NULL,
    playlist_id                 UUID                        NOT NULL,

    CONSTRAINT uk_playlist_subscriptions UNIQUE (user_id, playlist_id),

    CONSTRAINT fk_playlist_subscriptions_user_id     FOREIGN KEY (user_id)     REFERENCES users (id)     ON DELETE CASCADE,
    CONSTRAINT fk_playlist_subscriptions_playlist_id FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE
);

-- 알림 (notifications)
CREATE TABLE IF NOT EXISTS notifications
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    user_id                     UUID                        NOT NULL            REFERENCES users(id) ON DELETE CASCADE,
    title                       VARCHAR(255)                NOT NULL,
    content                     TEXT                        NOT NULL,
    level                       VARCHAR(20)                 NOT NULL            CHECK (level IN ('INFO', 'WARNING', 'ERROR')),
    is_read                     BOOLEAN                     NOT NULL            DEFAULT FALSE,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW()
);

-- 팔로우 (follows)
CREATE TABLE IF NOT EXISTS follows
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    follower_id                 UUID                        NOT NULL            REFERENCES users(id) ON DELETE CASCADE,
    followee_id                 UUID                        NOT NULL            REFERENCES users(id) ON DELETE CASCADE,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),

    UNIQUE(follower_id, followee_id)
);

-- 대화방 (conversations)
CREATE TABLE IF NOT EXISTS conversations
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    sender_id                   UUID                        NOT NULL            REFERENCES users(id) ON DELETE CASCADE,
    receiver_id                 UUID                        NOT NULL            REFERENCES users(id) ON DELETE CASCADE,
    has_unread                  BOOLEAN                     NOT NULL            DEFAULT FALSE,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),

    UNIQUE(sender_id, receiver_id)
);

-- DM (direct_messages)
CREATE TABLE IF NOT EXISTS direct_messages
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    conversation_id             UUID                        NOT NULL            REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id                   UUID                        NOT NULL            REFERENCES users(id) ON DELETE CASCADE,
    content                     TEXT                        NOT NULL,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW()
);

-- jwt
CREATE TABLE IF NOT EXISTS jwt
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    user_id                     UUID                        NOT NULL,
    created_at                  TIMESTAMPTZ                 NOT NULL,
    expires_at                  TIMESTAMPTZ                 NOT NULL,
    is_revoked                  BOOLEAN                     NOT NULL            DEFAULT FALSE,
    refresh_token               VARCHAR(255)                NOT NULL,

    CONSTRAINT fk_jwt_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 시청 세션 (watching_sessions)
CREATE TABLE IF NOT EXISTS WATCHING_SESSIONS
(
    id                          UUID            PRIMARY KEY NOT NULL            DEFAULT gen_random_uuid(),
    content_id                  UUID                        NOT NULL            REFERENCES contents(id) ON DELETE CASCADE,
    user_id                  UUID                        NOT NULL            REFERENCES users(id) ON DELETE CASCADE,
    created_at                  TIMESTAMPTZ                 NOT NULL            DEFAULT NOW(),

    UNIQUE (content_id, user_id)
);

-- INDEX
CREATE INDEX IF NOT EXISTS idx_jwt_token_user_id ON jwt(user_id);
CREATE INDEX IF NOT EXISTS idx_jwt_token_expires ON jwt(expires_at);

-- [Contents] 콘텐츠 커서 페이지네이션 및 검색 최적화
CREATE INDEX idx_contents_created_at ON contents (created_at DESC);
CREATE INDEX idx_contents_rating ON contents (avg_rating DESC);
-- JSONB 타입인 tags 컬럼 내의 배열 값을 빠르게 검색하기 위한 GIN 인덱스
CREATE INDEX idx_contents_tags ON contents USING GIN (tags);

-- [Playlists] 플레이리스트 커서 페이지네이션 최적화
CREATE INDEX idx_playlists_user_id ON playlists (user_id);
CREATE INDEX idx_playlists_updated_at ON playlists (updated_at DESC);
CREATE INDEX idx_playlists_subscriber_count ON playlists (subscriber_count DESC);

-- [Reviews] 특정 콘텐츠의 리뷰 목록 조회 (최신순 정렬 포함)
CREATE INDEX idx_reviews_content_created ON reviews (content_id, created_at DESC);

-- [Notifications] 유저별 알림 목록 조회 (최신순 정렬 포함)
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);

-- [Conversations] 유저가 속한 대화방 목록 조회 (최근 업데이트순)
CREATE INDEX idx_conversations_sender_id ON conversations (sender_id);
CREATE INDEX idx_conversations_receiver_id ON conversations (receiver_id);
CREATE INDEX idx_conversations_updated_at ON conversations (updated_at DESC);

-- [Direct Messages] 특정 대화방의 메시지 내역 조회 (최신순 정렬 포함)
CREATE INDEX idx_direct_messages_conv_created ON direct_messages (conversation_id, created_at DESC);

-- [Follows] 팔로워/팔로이 목록 조회 및 카운트 최적화
CREATE INDEX idx_follows_follower ON follows (follower_id);
CREATE INDEX idx_follows_followee ON follows (followee_id);

-- [Watching Sessions] 특정 콘텐츠의 현재 시청자 목록 조회
CREATE INDEX idx_watching_sessions_content ON watching_sessions (content_id);

-- [Playlist Contents & Subscriptions] 다대다 매핑 테이블 외래키 인덱스
CREATE INDEX idx_playlist_contents_playlist ON playlist_contents (playlist_id);
CREATE INDEX idx_playlist_subscriptions_playlist ON playlist_subscriptions (playlist_id);
