-- Tips, variations and footnotes, shown separately from the steps
ALTER TABLE recipe ADD COLUMN notes TEXT;

-- Optional heading an ingredient line belongs to, e.g. "For the dough". Consecutive lines with the
-- same group are shown together under it. ("group" itself is a reserved word in SQL.)
ALTER TABLE recipe_ingredient ADD COLUMN group_name VARCHAR(100);
