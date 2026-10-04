-- Calories and macros estimated for a whole recipe (all of its ingredients together).
-- source_hash is the hash of the ingredient lines the estimate was made from: when it differs from
-- the current lines, the estimate is outdated.
CREATE TABLE recipe_nutrition (
    recipe_id    UUID PRIMARY KEY REFERENCES recipe (id) ON DELETE CASCADE,
    kcal         INTEGER                     NOT NULL,
    protein_g    INTEGER                     NOT NULL,
    carbs_g      INTEGER                     NOT NULL,
    fat_g        INTEGER                     NOT NULL,
    source_hash  VARCHAR(64)                 NOT NULL,
    estimated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
