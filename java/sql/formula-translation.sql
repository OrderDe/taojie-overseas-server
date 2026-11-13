USE taojie_overseas;
CREATE TABLE IF NOT EXISTS eb_overseas_formula_translation (
 source_hash CHAR(64) NOT NULL,
 lang VARCHAR(8) NOT NULL,
 source_text TEXT NOT NULL,
 translated_text TEXT NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(source_hash,lang)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Formula display translation cache';
