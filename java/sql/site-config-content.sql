-- Expand site configuration storage for multilingual company profiles and image galleries.
-- Existing values are preserved. Run against taojie_overseas.
ALTER TABLE eb_overseas_site_config MODIFY COLUMN config_value MEDIUMTEXT NULL;
