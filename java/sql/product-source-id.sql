-- Preserve full external catalogue slugs used as stable import keys.
ALTER TABLE eb_overseas_product MODIFY COLUMN source_id VARCHAR(768) NOT NULL COMMENT 'External catalogue source identifier';
