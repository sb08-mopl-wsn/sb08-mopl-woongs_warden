CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE contents ADD COLUMN embedding vector(768);

CREATE INDEX idx_contents_embedding
    ON contents USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);