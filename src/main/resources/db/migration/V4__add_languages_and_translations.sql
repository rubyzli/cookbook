-- The language each recipe, ingredient and category was written in. Everything so far is Hungarian.
-- The default stays so that a backend from before this migration can still insert rows.
ALTER TABLE recipe ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'hu';
ALTER TABLE ingredient ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'hu';
ALTER TABLE category ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'hu';

-- One translation per recipe and language. status: MACHINE (unchecked) or REVIEWED.
-- source_hash fingerprints the original text it was made from, to spot later edits to the original.
CREATE TABLE recipe_translation (
    recipe_id    UUID NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    language     VARCHAR(5) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(255),
    instructions TEXT,
    notes        TEXT,
    status       VARCHAR(20) NOT NULL,
    source_hash  VARCHAR(64) NOT NULL,
    updated_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (recipe_id, language)
);

-- Translated ingredient group headings of a recipe translation, by original heading
CREATE TABLE recipe_translation_group (
    recipe_id  UUID NOT NULL,
    language   VARCHAR(5) NOT NULL,
    original   VARCHAR(100) NOT NULL,
    translated VARCHAR(100) NOT NULL,
    PRIMARY KEY (recipe_id, language, original),
    FOREIGN KEY (recipe_id, language) REFERENCES recipe_translation (recipe_id, language) ON DELETE CASCADE
);

CREATE TABLE ingredient_translation (
    ingredient_id UUID NOT NULL REFERENCES ingredient (id) ON DELETE CASCADE,
    language      VARCHAR(5) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    PRIMARY KEY (ingredient_id, language)
);

CREATE TABLE category_translation (
    category_id UUID NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    language    VARCHAR(5) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    PRIMARY KEY (category_id, language)
);
