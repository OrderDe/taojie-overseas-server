-- Migration for databases created before negotiable prices （面议） landed.
-- Run once against an existing overseas database:
--   mysql -u root -p taojie_overseas < sql/product-negotiable.sql
-- Safe to re-run: the ALTER will report "Duplicate column name 'negotiable'"
-- if it was already applied — that error is expected on a second run and
-- means there is nothing to do. Column position matches schema.sql
-- (AFTER ot_price). Existing rows get 0 （非面议）, so nothing changes for
-- products already in the catalogue.

ALTER TABLE eb_overseas_product
    ADD COLUMN negotiable TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否面议（1 = 前台显示面议、不显示数字价、不可加购）'
    AFTER ot_price;
