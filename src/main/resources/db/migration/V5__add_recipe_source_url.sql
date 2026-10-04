-- Where an imported (or copied) recipe comes from, shown as "Original recipe" on its page
ALTER TABLE recipe ADD COLUMN source_url VARCHAR(1000);
