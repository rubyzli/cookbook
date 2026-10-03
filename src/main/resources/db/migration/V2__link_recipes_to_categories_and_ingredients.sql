-- Deleting a category just untags its recipes
CREATE TABLE recipe_category (
    recipe_id   UUID NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES category (id) ON DELETE CASCADE,
    PRIMARY KEY (recipe_id, category_id)
);
CREATE INDEX recipe_category_category_idx ON recipe_category (category_id);

-- No unique constraint on (recipe_id, ingredient_id): a recipe can list the same
-- ingredient twice, e.g. butter for the dough and for the topping.
-- Deleting an ingredient that a recipe still uses is rejected.
CREATE TABLE recipe_ingredient (
    id            UUID PRIMARY KEY,
    recipe_id     UUID NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    ingredient_id UUID NOT NULL REFERENCES ingredient (id),
    amount        NUMERIC(10, 2),
    unit          VARCHAR(50),
    position      INTEGER NOT NULL
);
CREATE INDEX recipe_ingredient_recipe_idx ON recipe_ingredient (recipe_id);
CREATE INDEX recipe_ingredient_ingredient_idx ON recipe_ingredient (ingredient_id);
