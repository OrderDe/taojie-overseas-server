-- Demo catalogue for the Java implementation — generated from data/seed.json
-- (the same 14 sample products the interim Node service serves).
-- After real tjmall products are imported, this file is no longer needed.
USE taojie_overseas;

INSERT INTO eb_overseas_category (id, names, sort, is_show) VALUES
  ('paint', '{"en": "Automotive Paint", "zh": "汽车漆"}', 0, 1),
  ('primers', '{"en": "Primers", "zh": "底漆"}', 10, 1),
  ('clear', '{"en": "Clear Coats", "zh": "清漆"}', 20, 1),
  ('pearls', '{"en": "Pearls & Effects", "zh": "珠光与效果"}', 30, 1),
  ('home', '{"en": "Home & Living", "zh": "家居生活"}', 40, 1),
  ('appliances', '{"en": "Appliances", "zh": "小家电"}', 50, 1),
  ('paper', '{"en": "Paper & Tissue", "zh": "纸品"}', 60, 1),
  ('cleaning', '{"en": "Cleaning", "zh": "清洁用品"}', 70, 1)
ON DUPLICATE KEY UPDATE names = VALUES(names), sort = VALUES(sort), is_show = 1;

INSERT INTO eb_overseas_product
  (source_id, category_id, names, price, ot_price, sales, unit, moq, image, video, tag, blurb, specs, is_show, sort)
VALUES
  ('demo-1', 'clear', '{"en": "2K High-Gloss Clear Coat", "zh": "2K 高光清漆"}', 9.9, 14.5, 1280, '1L tin', 100, NULL, 'Best seller', '{"en": "Mirror gloss, fast cure, excellent weather resistance for refinish systems."}', '{"Mix ratio": "2 : 1 with hardener", "Drying time": "30 min @ 25°C (touch dry)", "Coverage": "8–10 m² / L", "Shelf life": "24 months"}', 1, 0),
  ('demo-2', 'paint', '{"en": "1K Metallic Basecoat — Silver", "zh": "1K 金属银底漆"}', 7.4, 10.2, 860, '1L tin', 100, NULL, NULL, '{"en": "Fine aluminium effect, strong hiding power, blends easily into OEM finishes."}', '{"Mix ratio": "Ready to use / 1 : 0.8 thinner", "Drying time": "15 min flash-off", "Coverage": "9–11 m² / L", "Shelf life": "24 months"}', 1, 10),
  ('demo-3', 'primers', '{"en": "Epoxy Primer — Grey", "zh": "环氧底漆 — 灰色"}', 8.6, 11.9, 640, '1L set', 100, NULL, NULL, '{"en": "Anti-rust epoxy barrier for bare metal, fibreglass and old paint films."}', '{"Mix ratio": "4 : 1 with epoxy hardener", "Drying time": "2 h sandable @ 25°C", "Coverage": "6–8 m² / L", "Shelf life": "18 months"}', 1, 20),
  ('demo-4', 'pearls', '{"en": "Pearl Pigment Set — 12 Colours", "zh": "珠光颜料套装 — 12 色"}', 18.9, 26, 430, 'set', 50, NULL, 'New', '{"en": "Gold, copper, interference and crystal pearls for custom colour effects."}', '{"Contents": "12 × 100 g jars", "Particle size": "10–60 µm", "Dosage": "3–8% of basecoat", "Shelf life": "36 months"}', 1, 30),
  ('demo-5', 'primers', '{"en": "F23 — 1K Grey Primer", "zh": "F23 — 1K 灰色底漆"}', 8.5, 12, 210, '1L tin', 100, NULL, NULL, '{"en": "Fast-sanding 1K primer surfacer, the workhorse under any Taojie basecoat."}', '{"Mix ratio": "Ready to use", "Drying time": "20 min sandable", "Coverage": "7–9 m² / L", "Shelf life": "24 months"}', 1, 40),
  ('demo-6', 'pearls', '{"en": "1K176 — Golden Pearl (Green Phase)", "zh": "1K176 — 黄金珠光（绿相）"}', 4.6, 6.8, 150, '100g jar', 200, NULL, NULL, '{"en": "Warm gold flop with a green phase shift — a signature Taojie effect pearl."}', '{"Particle size": "10–40 µm", "Dosage": "3–6% of basecoat", "Heat resistance": "180°C", "Shelf life": "36 months"}', 1, 50),
  ('demo-7', 'paint', '{"en": "2K278 — Bright Red Basecoat", "zh": "2K278 — 大红底漆"}', 4, 5.9, 175, '1L tin', 100, NULL, NULL, '{"en": "High-chroma solid red for 2K systems, strong coverage in two coats."}', '{"Mix ratio": "2 : 1 with hardener", "Drying time": "25 min @ 25°C", "Coverage": "8–10 m² / L", "Shelf life": "24 months"}', 1, 60),
  ('demo-8', 'primers', '{"en": "F25 — 1K White Primer", "zh": "F25 — 1K 白色底漆"}', 9.9, 13.5, 130, '1L tin', 100, NULL, NULL, '{"en": "Bright white ground coat that makes light and pearl colours pop."}', '{"Mix ratio": "Ready to use", "Drying time": "20 min sandable", "Coverage": "7–9 m² / L", "Shelf life": "24 months"}', 1, 70),
  ('demo-9', 'clear', '{"en": "2K801 — High-Gloss Clear Coat", "zh": "2K801 — 高光清漆"}', 2.4, 3.6, 320, '0.5L tin', 200, NULL, NULL, '{"en": "Compact-tin version of the flagship 2K clear — ideal sample and trial orders."}', '{"Mix ratio": "2 : 1 with hardener", "Drying time": "30 min @ 25°C", "Coverage": "8–10 m² / L", "Shelf life": "24 months"}', 1, 80),
  ('demo-10', 'paint', '{"en": "F2 — Plastic Primer", "zh": "F2 — 塑料底漆"}', 2.7, 4.1, 95, '1L tin', 100, NULL, NULL, '{"en": "Adhesion promoter for PP/ABS bumpers and trim — no sanding needed on new parts."}', '{"Mix ratio": "Ready to use", "Drying time": "10 min flash-off", "Coverage": "10–12 m² / L", "Shelf life": "18 months"}', 1, 90),
  ('demo-11', 'paper', '{"en": "Premium Facial Tissue — 6 Large Packs", "zh": "优质面巾纸 — 6 大包装"}', 1.8, 2.4, 5200, '6-pack', 500, NULL, 'High volume', '{"en": "5-ply virgin wood pulp, no fluorescent agents — the store’s daily-goods best seller."}', '{"Ply": "5", "Sheets": "420 / pack", "Material": "Virgin wood pulp", "Packaging": "6 packs / bag"}', 1, 100),
  ('demo-12', 'appliances', '{"en": "Electric Kettle — 1.8L Stainless", "zh": "电热水壶 — 1.8L 不锈钢"}', 6.9, 9.9, 940, 'pc', 200, NULL, NULL, '{"en": "1500W fast boil, dry-boil protection, food-grade 304 stainless interior."}', '{"Capacity": "1.8 L", "Power": "1500 W", "Voltage": "220–240 V", "Plug": "EU / UK / US on request"}', 1, 110),
  ('demo-13', 'appliances', '{"en": "Mini Rice Cooker — 1.6L", "zh": "迷你电饭煲 — 1.6L"}', 11.5, 16, 610, 'pc', 200, NULL, NULL, '{"en": "Compact cooker for dorms and small kitchens, non-stick inner pot."}', '{"Capacity": "1.6 L", "Power": "400 W", "Voltage": "220–240 V", "Functions": "Cook / Keep warm"}', 1, 120),
  ('demo-14', 'cleaning', '{"en": "Microfibre Cloths — 12 Pack", "zh": "超细纤维抹布 — 12 条装"}', 1.5, 2.2, 3100, '12-pack', 500, NULL, NULL, '{"en": "Lint-free detailing cloths — the same ones body shops use with Taojie coatings."}', '{"Size": "30 × 30 cm", "Weight": "300 GSM", "Blend": "80% polyester / 20% polyamide", "Colours": "Assorted"}', 1, 130)
ON DUPLICATE KEY UPDATE names = VALUES(names), price = VALUES(price), ot_price = VALUES(ot_price),
  sales = VALUES(sales), unit = VALUES(unit), moq = VALUES(moq), image = VALUES(image),
  tag = VALUES(tag), blurb = VALUES(blurb), specs = VALUES(specs), is_show = 1;
