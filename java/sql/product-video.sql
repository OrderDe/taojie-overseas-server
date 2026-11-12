-- Migration for databases created before product videos landed.
-- Run once against an existing overseas database:
--   mysql -u root -p taojie_overseas < sql/product-video.sql
-- Safe to re-run: the ALTER will report "Duplicate column name 'video'"
-- if it was already applied — that error is expected on a second run and
-- means there is nothing to do. Column position matches schema.sql
-- (AFTER image). Existing rows get NULL, which the API returns as ''.

ALTER TABLE eb_overseas_product
    ADD COLUMN video VARCHAR(1024) NULL COMMENT '商品视频 URL（空 = 无视频）'
    AFTER image;
