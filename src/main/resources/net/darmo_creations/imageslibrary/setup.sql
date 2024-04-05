-- Current schema version, should match the value of DatabaseConnection.CURRENT_SCHEMA_VERSION
PRAGMA USER_VERSION = 0;

CREATE TABLE images
(
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    path TEXT UNIQUE NOT NULL,
    hash INTEGER     NOT NULL
) STRICT;

CREATE INDEX idx_images_hash ON images (hash); -- Speed up hash querying

CREATE TABLE tag_types
(
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    label  TEXT NOT NULL UNIQUE,
    symbol TEXT NOT NULL UNIQUE,
    color  INTEGER DEFAULT 0
) STRICT;

CREATE TABLE tags
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    label      TEXT UNIQUE NOT NULL,
    type_id    INTEGER DEFAULT NULL,
    definition TEXT    DEFAULT NULL,
    FOREIGN KEY (type_id) REFERENCES tag_types (id) ON DELETE SET NULL
) STRICT;

CREATE TABLE image_tag
(
    image_id INTEGER NOT NULL,
    tag_id   INTEGER NOT NULL,
    PRIMARY KEY (image_id, tag_id),
    FOREIGN KEY (image_id) REFERENCES images (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
) STRICT;
