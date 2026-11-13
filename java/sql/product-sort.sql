-- Migration for databases created before the product sort column
-- landed (商品排序：数值越大越靠前，公开与后台列表均按 sort DESC, id DESC).
-- Run once against an existing overseas database:
--   mysql -u root -p taojie_overseas < sql/product-sort.sql
-- Safe to re-run: the ALTER will report "Duplicate column name 'sort'"
-- if it was already applied — that error is expected on a second run
-- and means there is nothing to do. Column position matches schema.sql
-- (AFTER is_show). Existing rows get 0, so their relative order is
-- unchanged until an admin sets sort values in the product list.

ALTER TABLE eb_overseas_product
    ADD COLUMN sort INT NOT NULL DEFAULT 0 COMMENT '排序（数值越大越靠前）'
    AFTER is_show;
