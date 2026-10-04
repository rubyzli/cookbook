CREATE TABLE category (
    id   UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX category_name_lower_idx ON category (lower(name));

CREATE TABLE ingredient (
    id   UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX ingredient_name_lower_idx ON ingredient (lower(name));

CREATE TABLE recipe (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    description       VARCHAR(255),
    servings          INTEGER,
    prep_time_minutes INTEGER,
    cook_time_minutes INTEGER,
    instructions      TEXT,
    image_url         VARCHAR(255),
    created_by        VARCHAR(255),
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX recipe_name_lower_idx ON recipe (lower(name));
