---------------------------------------------------------------
-- CATEGORIES (Laptop-focused)
---------------------------------------------------------------
INSERT INTO dbo.categories (name, description, parent_id, display_order, is_active)
VALUES
(N'Laptop',            N'Sản phẩm laptop',          NULL, 1, 1),
(N'Laptop Gaming',     N'Laptop chơi game',         1,    1, 1),
(N'Laptop Văn Phòng',  N'Laptop học tập – văn phòng',1,   2, 1),
(N'Laptop Doanh Nhân', N'Laptop cao cấp',           1,    3, 1),
(N'Phụ Kiện',          N'Phụ kiện laptop',          NULL, 2, 1);
GO

---------------------------------------------------------------
-- TAGS
---------------------------------------------------------------
INSERT INTO dbo.tags (name, tag_type, is_active)
VALUES
(N'Gaming',      N'PRODUCT', 1),
(N'Văn Phòng',   N'PRODUCT', 1),
(N'Bán Chạy',    N'PRODUCT', 1),
(N'Mới Về',      N'PRODUCT', 1),
(N'Cao Cấp',     N'PRODUCT', 1);
GO

---------------------------------------------------------------
-- PRODUCTS (Laptop)
---------------------------------------------------------------
INSERT INTO dbo.products (name, sku, description, is_visible, is_featured)
VALUES
(N'ASUS ROG Strix G15', N'LAP-ASUS-ROG-G15', N'Laptop gaming hiệu năng cao', 1, 1),
(N'Dell Inspiron 15',  N'LAP-DELL-INS-15',  N'Laptop văn phòng phổ thông',  1, 0),
(N'HP EliteBook 840',  N'LAP-HP-EB-840',    N'Laptop doanh nhân cao cấp',   1, 1);
GO

---------------------------------------------------------------
-- PRODUCT CATEGORIES
---------------------------------------------------------------
INSERT INTO dbo.product_categories (product_id, category_id, is_primary)
VALUES
((SELECT id FROM dbo.products WHERE sku = N'LAP-ASUS-ROG-G15'),
 (SELECT id FROM dbo.categories WHERE name = N'Laptop Gaming'), 1),

((SELECT id FROM dbo.products WHERE sku = N'LAP-DELL-INS-15'),
 (SELECT id FROM dbo.categories WHERE name = N'Laptop Văn Phòng'), 1),

((SELECT id FROM dbo.products WHERE sku = N'LAP-HP-EB-840'),
 (SELECT id FROM dbo.categories WHERE name = N'Laptop Doanh Nhân'), 1);
GO

---------------------------------------------------------------
-- PRODUCT TAGS
---------------------------------------------------------------
INSERT INTO dbo.product_tags (product_id, tag_id)
VALUES
((SELECT id FROM dbo.products WHERE sku = N'LAP-ASUS-ROG-G15'),
 (SELECT id FROM dbo.tags WHERE name = N'Gaming')),

((SELECT id FROM dbo.products WHERE sku = N'LAP-ASUS-ROG-G15'),
 (SELECT id FROM dbo.tags WHERE name = N'Bán Chạy')),

((SELECT id FROM dbo.products WHERE sku = N'LAP-DELL-INS-15'),
 (SELECT id FROM dbo.tags WHERE name = N'Văn Phòng')),

((SELECT id FROM dbo.products WHERE sku = N'LAP-HP-EB-840'),
 (SELECT id FROM dbo.tags WHERE name = N'Cao Cấp'));
GO

---------------------------------------------------------------
-- PRODUCT VARIANTS
---------------------------------------------------------------
INSERT INTO dbo.product_variants
(product_id, variant_name, sku, currency_code, price, cost_price, stock_quantity, is_active)
VALUES
(
 (SELECT id FROM dbo.products WHERE sku = N'LAP-ASUS-ROG-G15'),
 N'Ryzen 7 / 16GB / 1TB / RTX 3060',
 N'LAP-ASUS-ROG-G15-R7',
 'VND',
 38990000,
 35000000,
 12,
 1
),
(
 (SELECT id FROM dbo.products WHERE sku = N'LAP-DELL-INS-15'),
 N'Core i5 / 8GB / 512GB',
 N'LAP-DELL-INS-15-I5',
 'VND',
 18990000,
 17000000,
 20,
 1
),
(
 (SELECT id FROM dbo.products WHERE sku = N'LAP-HP-EB-840'),
 N'Core i7 / 16GB / 512GB',
 N'LAP-HP-EB-840-I7',
 'VND',
 32990000,
 30000000,
 8,
 1
);
GO

---------------------------------------------------------------
-- IMAGES
---------------------------------------------------------------
INSERT INTO dbo.images (product_id, url, is_primary, sort_order)
VALUES
((SELECT id FROM dbo.products WHERE sku = N'LAP-ASUS-ROG-G15'),
 N'https://cdn.yoursite.com/laptops/asus-rog-g15.jpg', 1, 0),

((SELECT id FROM dbo.products WHERE sku = N'LAP-DELL-INS-15'),
 N'https://cdn.yoursite.com/laptops/dell-inspiron-15.jpg', 1, 0),

((SELECT id FROM dbo.products WHERE sku = N'LAP-HP-EB-840'),
 N'https://cdn.yoursite.com/laptops/hp-elitebook-840.jpg', 1, 0);
GO

---------------------------------------------------------------
-- PRICE HISTORY
---------------------------------------------------------------
INSERT INTO dbo.price_history (variant_id, price, cost_price, reason, created_by)
VALUES
(
 (SELECT id FROM dbo.product_variants WHERE sku = N'LAP-ASUS-ROG-G15-R7'),
 38990000,
 35000000,
 N'Giá khởi tạo',
 (SELECT id FROM dbo.users WHERE username = N'admin')
),
(
 (SELECT id FROM dbo.product_variants WHERE sku = N'LAP-DELL-INS-15-I5'),
 18990000,
 17000000,
 N'Giá khởi tạo',
 (SELECT id FROM dbo.users WHERE username = N'admin')
);
GO
