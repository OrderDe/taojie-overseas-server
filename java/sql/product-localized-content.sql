-- Apply once before starting the updated backend.
ALTER TABLE eb_overseas_product ADD COLUMN localized_content JSON NULL COMMENT 'Localized unit, tag and specs';
