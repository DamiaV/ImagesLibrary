-- Current schema version, should match the value of DatabaseConnection.CURRENT_SCHEMA_VERSION
PRAGMA USER_VERSION = 0;

CREATE TABLE images
(
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    path TEXT UNIQUE NOT NULL,
    hash INTEGER     NOT NULL
) STRICT;

-- Speed up hash querying
CREATE INDEX idx_images_hash ON images (hash);

-- The `updating` column allows swapping the labels and/or symbols of several tag types in the same transaction.
CREATE TABLE tag_types
(
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    label    TEXT NOT NULL,
    symbol   TEXT NOT NULL,
    color    INTEGER DEFAULT 0,
    updating INTEGER DEFAULT 0,
    UNIQUE (label, updating),
    UNIQUE (symbol, updating)
) STRICT;

-- The `updating` column allows swapping the labels of several tags in the same transaction.
CREATE TABLE tags
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    label      TEXT NOT NULL,
    type_id    INTEGER DEFAULT NULL,
    definition TEXT    DEFAULT NULL,
    updating   INTEGER DEFAULT 0,
    UNIQUE (label, updating),
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

CREATE TABLE saved_queries
(
    name    TEXT PRIMARY KEY,
    `query` TEXT    NOT NULL UNIQUE,
    `order` INTEGER NOT NULL UNIQUE
) STRICT;

CREATE TABLE batch_operations
(
    name TEXT PRIMARY KEY
) STRICT;

CREATE TABLE image_operation
(
    type           TEXT    NOT NULL,
    data           TEXT DEFAULT NULL,
    condition_type TEXT DEFAULT NULL,
    condition_data TEXT DEFAULT NULL,
    `order`        INTEGER NOT NULL,
    batch_name     TEXT    NOT NULL,
    UNIQUE (batch_name, `order`),
    FOREIGN KEY (batch_name) REFERENCES batch_operations (name) ON DELETE CASCADE
) STRICT;
