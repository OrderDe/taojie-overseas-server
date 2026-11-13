-- Apply once before starting the three-level category backend. Existing categories remain roots until relationships are migrated.
ALTER TABLE eb_overseas_category ADD COLUMN parent_id VARCHAR(64) NOT NULL DEFAULT '' AFTER id, ADD INDEX idx_category_parent (parent_id);
