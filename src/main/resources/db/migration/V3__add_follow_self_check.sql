ALTER TABLE follows
ADD CONSTRAINT ck_follows_not_self
CHECK (follower_id <> followee_id);
