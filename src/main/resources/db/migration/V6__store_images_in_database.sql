-- Recipe photos are kept in the database, so they survive redeploys and move with backups.
-- A recipe's image_url points at one as /api/images/{id}.
CREATE TABLE image (
    id           UUID PRIMARY KEY,
    content_type VARCHAR(50)                 NOT NULL,
    data         BYTEA                       NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
