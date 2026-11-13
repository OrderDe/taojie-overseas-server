#!/usr/bin/env python3
"""Import products into the overseas MySQL database (Java implementation).

Usage:
    DB_HOST=127.0.0.1 DB_USER=root DB_PASSWORD=xxx DB_NAME=taojie_overseas \
        python3 import-products.py products.json

Input JSON format:

    {
      "categories": [{"id": "clear", "names": {"en": "...", "zh": "..."}}],
      "products":   [{"sourceId": "tjmall-123", "categoryId": "clear",
                      "names": {"en": "...", "zh": "..."}, "price": 9.9,
                      "otPrice": 14.5, "sales": 0, "unit": "1L tin",
                      "moq": 100, "image": "https://...", "video": "", "tag": "",
                      "blurb": {"en": "..."}, "specs": {"Mix ratio": "2 : 1"}}]
    }

Rows are upserted by source_id.
`blurb` may be a plain string (stored as single-language text) or a map.

Requires: pymysql  (pip install pymysql)
The tjmall export step only has to produce this JSON once — fields it
cannot supply (English names, USD prices, MOQ) are filled before import.
"""
import json
import os
import sys

import pymysql


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2
    with open(sys.argv[1], encoding="utf-8") as f:
        data = json.load(f)

    conn = pymysql.connect(
        host=os.environ.get("DB_HOST", "127.0.0.1"),
        port=int(os.environ.get("DB_PORT", "3306")),
        user=os.environ.get("DB_USER", "root"),
        password=os.environ.get("DB_PASSWORD", ""),
        database=os.environ.get("DB_NAME", "taojie_overseas"),
        charset="utf8mb4",
        autocommit=True,
    )
    with conn.cursor() as cur:
        for c in data.get("categories", []):
            cur.execute(
                """INSERT INTO eb_overseas_category (id, names, sort, is_show)
                   VALUES (%s, %s, %s, 1)
                   ON DUPLICATE KEY UPDATE names = VALUES(names), is_show = 1""",
                (c["id"], json.dumps(c["names"], ensure_ascii=False), c.get("sort", 0)),
            )
        n = 0
        for p in data.get("products", []):
            blurb = p.get("blurb")
            blurb_json = json.dumps(blurb, ensure_ascii=False) if isinstance(blurb, (dict, list)) else blurb
            cur.execute(
                """INSERT INTO eb_overseas_product
                     (source_id, category_id, names, price, ot_price, sales, unit, moq,
                      image, video, tag, blurb, specs, is_show, sort)
                   VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 1, %s)
                   ON DUPLICATE KEY UPDATE
                     category_id = VALUES(category_id), names = VALUES(names),
                     price = VALUES(price), ot_price = VALUES(ot_price),
                     sales = VALUES(sales), unit = VALUES(unit), moq = VALUES(moq),
                     image = VALUES(image), video = VALUES(video), tag = VALUES(tag),
                     blurb = VALUES(blurb),
                     specs = VALUES(specs), is_show = 1""",
                (
                    p["sourceId"],
                    p["categoryId"],
                    json.dumps(p["names"], ensure_ascii=False),
                    p.get("price", 0),
                    p.get("otPrice", 0),
                    p.get("sales", 0),
                    p.get("unit"),
                    p.get("moq", 1),
                    p.get("image") or None,
                    p.get("video") or None,
                    p.get("tag") or None,
                    blurb_json,
                    json.dumps(p.get("specs"), ensure_ascii=False) if p.get("specs") else None,
                    p.get("sort", 0),
                ),
            )
            n += 1
    conn.close()
    print(f"imported {len(data.get('categories', []))} categories, {n} products")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
