-- Existing single-image/video columns remain available as covers.
ALTER TABLE eb_overseas_product ADD COLUMN images JSON NULL, ADD COLUMN videos JSON NULL;
