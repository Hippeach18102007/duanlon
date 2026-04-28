SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

CREATE DATABASE retail_management_v5;
GO
USE retail_management_v5;
GO

---------------------------------------------------------------
-- SEQUENCES
---------------------------------------------------------------
CREATE SEQUENCE dbo.order_seq
    AS BIGINT
    START WITH 1000
    INCREMENT BY 1;
GO

---------------------------------------------------------------
-- 1) USERS / AUTH / AUDIT
---------------------------------------------------------------
CREATE TABLE dbo.users (
    id            INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_users PRIMARY KEY,
    username      NVARCHAR(50)  NOT NULL,
    email         NVARCHAR(100) NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    role          NVARCHAR(30)  NOT NULL CONSTRAINT DF_users_role DEFAULT N'CUSTOMER',
    is_active     BIT NOT NULL CONSTRAINT DF_users_is_active DEFAULT 1,
    created_at    DATETIME2 NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSDATETIME(),
    updated_at    DATETIME2 NOT NULL CONSTRAINT DF_users_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_users_username UNIQUE (username),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT CK_users_role CHECK (role IN (N'ADMIN', N'SALES', N'INVENTORY', N'CUSTOMER'))
);
GO

CREATE TABLE dbo.user_logins (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_user_logins PRIMARY KEY,
    user_id    INT NULL,
    username   NVARCHAR(50) NULL,
    success    BIT NOT NULL,
    ip_address NVARCHAR(45) NULL,
    user_agent NVARCHAR(400) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_user_logins_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NULL CONSTRAINT DF_user_logins_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_user_logins_user FOREIGN KEY (user_id)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_user_logins_user_date ON dbo.user_logins(user_id, created_at DESC);
CREATE INDEX IX_user_logins_success_date ON dbo.user_logins(success, created_at DESC);
GO

CREATE TABLE dbo.password_resets (
    id           BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_password_resets PRIMARY KEY,
    user_id      INT NOT NULL,
    token_hash   NVARCHAR(255) NOT NULL,
    expires_at   DATETIME2 NOT NULL,
    used_at      DATETIME2 NULL,
    requested_ip NVARCHAR(45) NULL,
    created_at   DATETIME2 NOT NULL CONSTRAINT DF_password_resets_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_password_resets_user FOREIGN KEY (user_id)
        REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO
CREATE INDEX IX_password_resets_user_date ON dbo.password_resets(user_id, created_at DESC);
GO

CREATE TABLE dbo.audit_logs (
    id           BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_audit_logs PRIMARY KEY,
    user_id      INT NULL,
    module       NVARCHAR(50) NOT NULL,
    action       NVARCHAR(50) NOT NULL,
    target_type  NVARCHAR(50) NULL,
    target_id    BIGINT NULL,
    details_json NVARCHAR(MAX) NULL,
    ip_address   NVARCHAR(45) NULL,
    created_at   DATETIME2 NOT NULL CONSTRAINT DF_audit_logs_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_audit_user FOREIGN KEY (user_id)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_audit_user_date   ON dbo.audit_logs(user_id, created_at DESC);
CREATE INDEX IX_audit_module_date ON dbo.audit_logs(module, created_at DESC);
CREATE INDEX IX_audit_target      ON dbo.audit_logs(target_type, target_id, created_at DESC);
GO

---------------------------------------------------------------
-- 2) CUSTOMERS + LOYALTY
---------------------------------------------------------------
CREATE TABLE dbo.customers (
    id             INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_customers PRIMARY KEY,
    user_id         INT NULL,
    name           NVARCHAR(150) NOT NULL,
    email          NVARCHAR(100) NULL,
    phone          NVARCHAR(30)  NULL,
    date_of_birth  DATE NULL,
    customer_type  NVARCHAR(20) NOT NULL CONSTRAINT DF_customers_customer_type DEFAULT N'REGULAR',
    vip_tier       NVARCHAR(30) NULL,
    segments_json  NVARCHAR(MAX) NULL,
    loyalty_points INT NOT NULL CONSTRAINT DF_customers_loyalty_points DEFAULT 0,
    total_spent    DECIMAL(15,2) NOT NULL CONSTRAINT DF_customers_total_spent DEFAULT 0,
    last_order_at  DATETIME2 NULL,
    last_login_at  DATETIME2 NULL,
    address        NVARCHAR(500) NULL,
    notes          NVARCHAR(MAX) NULL,
    is_active      BIT NOT NULL CONSTRAINT DF_customers_is_active DEFAULT 1,
    created_at     DATETIME2 NOT NULL CONSTRAINT DF_customers_created_at DEFAULT SYSDATETIME(),
    updated_at     DATETIME2 NOT NULL CONSTRAINT DF_customers_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_customers_user UNIQUE (user_id),
    CONSTRAINT UQ_customers_email UNIQUE (email),
    CONSTRAINT FK_customers_user FOREIGN KEY (user_id)
        REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_customers_type ON dbo.customers(customer_type);
CREATE INDEX IX_customers_points ON dbo.customers(loyalty_points DESC);
CREATE INDEX IX_customers_spent ON dbo.customers(total_spent DESC);
CREATE INDEX IX_customers_last_order ON dbo.customers(last_order_at DESC);
CREATE INDEX IX_customers_last_login ON dbo.customers(last_login_at DESC);
GO

CREATE TABLE dbo.loyalty_ledger (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_loyalty_ledger PRIMARY KEY,
    customer_id    INT NOT NULL,
    points_delta   INT NOT NULL,
    reason         NVARCHAR(200) NULL,
    reference_type NVARCHAR(50) NULL,
    reference_id   BIGINT NULL,
    note           NVARCHAR(500) NULL,
    created_by     INT NULL,
    created_at     DATETIME2 NOT NULL CONSTRAINT DF_loyalty_ledger_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_loyalty_customer FOREIGN KEY (customer_id)
        REFERENCES dbo.customers(id) ON DELETE CASCADE,
    CONSTRAINT FK_loyalty_user FOREIGN KEY (created_by)
        REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_loyalty_customer_date ON dbo.loyalty_ledger(customer_id, created_at DESC);
GO

---------------------------------------------------------------
-- 3) CATEGORIES & TAGS
---------------------------------------------------------------
CREATE TABLE dbo.categories (
    id            INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_categories PRIMARY KEY,
    name          NVARCHAR(150) NOT NULL,
    description   NVARCHAR(500) NULL,
    image_url     NVARCHAR(500) NULL,
    parent_id     INT NULL,
    display_order INT NOT NULL CONSTRAINT DF_categories_display_order DEFAULT 0,
    is_active     BIT NOT NULL CONSTRAINT DF_categories_is_active DEFAULT 1,
    created_at    DATETIME2 NOT NULL CONSTRAINT DF_categories_created_at DEFAULT SYSDATETIME(),
    updated_at    DATETIME2 NOT NULL CONSTRAINT DF_categories_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_categories_parent FOREIGN KEY (parent_id)
        REFERENCES dbo.categories(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_categories_parent ON dbo.categories(parent_id);
GO

CREATE TABLE dbo.tags (
    id         INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_tags PRIMARY KEY,
    name       NVARCHAR(80) NOT NULL,
    tag_type   NVARCHAR(30) NOT NULL CONSTRAINT DF_tags_tag_type DEFAULT N'GENERAL',
    is_active  BIT NOT NULL CONSTRAINT DF_tags_is_active DEFAULT 1,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_tags_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_tags_name UNIQUE (name)
);
GO

---------------------------------------------------------------
-- 4) PRODUCTS & VARIANTS
---------------------------------------------------------------
CREATE TABLE dbo.products (
    id              INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_products PRIMARY KEY,
    name            NVARCHAR(200) NOT NULL,
    sku             NVARCHAR(100) NOT NULL,
    description     NVARCHAR(MAX) NULL,
    is_visible      BIT NOT NULL CONSTRAINT DF_products_is_visible DEFAULT 1,
    is_featured     BIT NOT NULL CONSTRAINT DF_products_is_featured DEFAULT 0,
    view_count      INT NOT NULL CONSTRAINT DF_products_view_count DEFAULT 0,
    sold_count      INT NOT NULL CONSTRAINT DF_products_sold_count DEFAULT 0,
    attributes_json NVARCHAR(MAX) NULL,
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_products_created_at DEFAULT SYSDATETIME(),
    updated_at      DATETIME2 NOT NULL CONSTRAINT DF_products_updated_at DEFAULT SYSDATETIME(),
    deleted_at      DATETIME2 NULL,
    CONSTRAINT UQ_products_sku UNIQUE (sku)
);
GO
CREATE INDEX IX_products_visible ON dbo.products(is_visible, created_at DESC);
CREATE INDEX IX_products_created ON dbo.products(created_at DESC);
CREATE INDEX IX_products_sold    ON dbo.products(sold_count DESC);
CREATE INDEX IX_products_name    ON dbo.products(name);
GO

CREATE TABLE dbo.product_categories (
    product_id  INT NOT NULL,
    category_id INT NOT NULL,
    is_primary  BIT NOT NULL CONSTRAINT DF_product_categories_is_primary DEFAULT 0,
    created_at  DATETIME2 NOT NULL CONSTRAINT DF_product_categories_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_product_categories PRIMARY KEY (product_id, category_id),
    CONSTRAINT FK_pc_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_pc_category FOREIGN KEY (category_id)
        REFERENCES dbo.categories(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_pc_category ON dbo.product_categories(category_id, product_id);
GO

CREATE TABLE dbo.product_tags (
    product_id INT NOT NULL,
    tag_id     INT NOT NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_product_tags_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_product_tags PRIMARY KEY (product_id, tag_id),
    CONSTRAINT FK_pt_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_pt_tag FOREIGN KEY (tag_id)
        REFERENCES dbo.tags(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_pt_tag ON dbo.product_tags(tag_id, product_id);
GO

CREATE TABLE dbo.product_variants (
    id               INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_product_variants PRIMARY KEY,
    product_id       INT NOT NULL,
    variant_name     NVARCHAR(150) NOT NULL,
    sku              NVARCHAR(100) NOT NULL,
    barcode          NVARCHAR(100) NULL,
    currency_code    CHAR(3) NOT NULL CONSTRAINT DF_product_variants_currency_code DEFAULT 'VND',
    price            DECIMAL(12,2) NOT NULL,
    cost_price       DECIMAL(12,2) NULL,
    price_tiers_json NVARCHAR(MAX) NULL,
    stock_quantity   INT NOT NULL CONSTRAINT DF_product_variants_stock_quantity DEFAULT 0,
    reserved_qty     INT NOT NULL CONSTRAINT DF_product_variants_reserved_qty DEFAULT 0,
    attributes_json  NVARCHAR(MAX) NULL,
    is_active        BIT NOT NULL CONSTRAINT DF_product_variants_is_active DEFAULT 1,
    created_at       DATETIME2 NOT NULL CONSTRAINT DF_product_variants_created_at DEFAULT SYSDATETIME(),
    updated_at       DATETIME2 NOT NULL CONSTRAINT DF_product_variants_updated_at DEFAULT SYSDATETIME(),
    deleted_at       DATETIME2 NULL,
    CONSTRAINT UQ_product_variants_sku UNIQUE (sku),
    CONSTRAINT FK_variants_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO
CREATE INDEX IX_variants_product ON dbo.product_variants(product_id);
CREATE INDEX IX_variants_stock   ON dbo.product_variants(stock_quantity, reserved_qty);
CREATE INDEX IX_variants_created ON dbo.product_variants(created_at DESC);
GO

CREATE TABLE dbo.images (
    id         BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_images PRIMARY KEY,
    product_id INT NULL,
    variant_id INT NULL,
    url        NVARCHAR(800) NOT NULL,
    is_primary BIT NOT NULL CONSTRAINT DF_images_is_primary DEFAULT 0,
    sort_order INT NOT NULL CONSTRAINT DF_images_sort_order DEFAULT 0,
    alt_text   NVARCHAR(200) NULL,
    meta_json  NVARCHAR(MAX) NULL,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_images_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_images_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_images_variant FOREIGN KEY (variant_id)
        REFERENCES dbo.product_variants(id) ON DELETE NO ACTION,
    CONSTRAINT CK_images_owner CHECK (
        (product_id IS NOT NULL AND variant_id IS NULL) OR
        (product_id IS NULL AND variant_id IS NOT NULL)
    )
);
GO
CREATE INDEX IX_images_product ON dbo.images(product_id, is_primary DESC, sort_order, id DESC);
CREATE INDEX IX_images_variant ON dbo.images(variant_id, is_primary DESC, sort_order, id DESC);
GO

---------------------------------------------------------------
-- 5) INVENTORY TRANSACTIONS
---------------------------------------------------------------
CREATE TABLE dbo.stock_transactions (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_stock_transactions PRIMARY KEY,
    variant_id     INT NOT NULL,
    quantity       INT NOT NULL,
    type           NVARCHAR(30) NOT NULL,
    reference_type NVARCHAR(50) NULL,
    reference_id   BIGINT NULL,
    note           NVARCHAR(500) NULL,
    created_by     INT NULL,
    created_at     DATETIME2 NOT NULL CONSTRAINT DF_stock_transactions_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_st_variant FOREIGN KEY (variant_id)
        REFERENCES dbo.product_variants(id) ON DELETE CASCADE,
    CONSTRAINT FK_st_user FOREIGN KEY (created_by)
        REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_st_variant_date ON dbo.stock_transactions(variant_id, created_at DESC);
CREATE INDEX IX_st_ref ON dbo.stock_transactions(reference_type, reference_id);
GO

---------------------------------------------------------------
-- 6) PROMOTIONS + PRICE HISTORY
---------------------------------------------------------------
CREATE TABLE dbo.promotions (
    id                 INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_promotions PRIMARY KEY,
    code               NVARCHAR(50) NOT NULL,
    name               NVARCHAR(150) NOT NULL,
    description        NVARCHAR(500) NULL,
    discount_type      NVARCHAR(20) NOT NULL,
    discount_value     DECIMAL(12,2) NOT NULL,
    min_order_amount   DECIMAL(15,2) NULL,
    applicability_json NVARCHAR(MAX) NULL,
    rules_json         NVARCHAR(MAX) NULL,
    priority           INT NOT NULL CONSTRAINT DF_promotions_priority DEFAULT 0,
    stackable          BIT NOT NULL CONSTRAINT DF_promotions_stackable DEFAULT 0,
    start_date         DATETIME2 NOT NULL,
    end_date           DATETIME2 NOT NULL,
    usage_limit        INT NULL,
    used_count         INT NOT NULL CONSTRAINT DF_promotions_used_count DEFAULT 0,
    is_active          BIT NOT NULL CONSTRAINT DF_promotions_is_active DEFAULT 1,
    created_by         INT NULL,
    created_at         DATETIME2 NOT NULL CONSTRAINT DF_promotions_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_promotions_code UNIQUE (code),
    CONSTRAINT FK_promotions_user FOREIGN KEY (created_by)
        REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_promotions_active_dates ON dbo.promotions(is_active, start_date, end_date);
GO

CREATE TABLE dbo.price_history (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_price_history PRIMARY KEY,
    variant_id     INT NOT NULL,
    currency_code  CHAR(3) NOT NULL CONSTRAINT DF_price_history_currency_code DEFAULT 'VND',
    price          DECIMAL(12,2) NOT NULL,
    cost_price     DECIMAL(12,2) NULL,
    reason         NVARCHAR(200) NULL,
    effective_from DATETIME2 NOT NULL CONSTRAINT DF_price_history_effective_from DEFAULT SYSDATETIME(),
    effective_to   DATETIME2 NULL,
    created_by     INT NULL,
    created_at     DATETIME2 NOT NULL CONSTRAINT DF_price_history_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_ph_variant FOREIGN KEY (variant_id)
        REFERENCES dbo.product_variants(id) ON DELETE CASCADE,
    CONSTRAINT FK_ph_user FOREIGN KEY (created_by)
        REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_ph_variant_date ON dbo.price_history(variant_id, effective_from DESC);
GO

---------------------------------------------------------------
-- 7) ORDERS / ITEMS / PAYMENTS / RETURNS
---------------------------------------------------------------
CREATE TABLE dbo.orders (
    id             BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_orders PRIMARY KEY,
    order_number   NVARCHAR(40) NOT NULL
        CONSTRAINT DF_orders_order_number DEFAULT (
            CONCAT(
                N'ORD-',
                CONVERT(CHAR(8), SYSDATETIME(), 112),
                N'-',
                RIGHT(N'000000' + CONVERT(VARCHAR(20), NEXT VALUE FOR dbo.order_seq), 6)
            )
        ),
    customer_id    INT NULL,
    user_id        INT NULL,
    channel        NVARCHAR(20) NOT NULL CONSTRAINT DF_orders_channel DEFAULT N'OFFLINE',
    status         NVARCHAR(30) NOT NULL CONSTRAINT DF_orders_status DEFAULT N'PENDING',
    payment_method NVARCHAR(50) NULL,
    payment_status NVARCHAR(20) NOT NULL CONSTRAINT DF_orders_payment_status DEFAULT N'UNPAID',
    shipping_address NVARCHAR(500) NULL,
    shipping_fee   DECIMAL(12,2) NOT NULL CONSTRAINT DF_orders_shipping_fee DEFAULT 0,
    shipping_status NVARCHAR(30) NULL,
    tracking_code  NVARCHAR(100) NULL,
    paid_at        DATETIME2 NULL,
    shipped_at     DATETIME2 NULL,
    delivered_at   DATETIME2 NULL,
    cancelled_at   DATETIME2 NULL,
    returned_at    DATETIME2 NULL,
    subtotal       DECIMAL(15,2) NOT NULL CONSTRAINT DF_orders_subtotal DEFAULT 0,
    discount_total DECIMAL(15,2) NOT NULL CONSTRAINT DF_orders_discount_total DEFAULT 0,
    tax_total      DECIMAL(15,2) NOT NULL CONSTRAINT DF_orders_tax_total DEFAULT 0,
    total_amount   DECIMAL(15,2) NOT NULL CONSTRAINT DF_orders_total_amount DEFAULT 0,
    applied_promotion_code NVARCHAR(50) NULL,
    applied_promotion_json NVARCHAR(MAX) NULL,
    notes          NVARCHAR(MAX) NULL,
    created_at     DATETIME2 NOT NULL CONSTRAINT DF_orders_created_at DEFAULT SYSDATETIME(),
    updated_at     DATETIME2 NOT NULL CONSTRAINT DF_orders_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_orders_order_number UNIQUE (order_number),
    CONSTRAINT FK_orders_customer FOREIGN KEY (customer_id)
        REFERENCES dbo.customers(id) ON DELETE SET NULL,
    CONSTRAINT FK_orders_user FOREIGN KEY (user_id)
        REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_orders_status_date ON dbo.orders(status, created_at DESC);
CREATE INDEX IX_orders_customer_date ON dbo.orders(customer_id, created_at DESC);
CREATE INDEX IX_orders_user_date ON dbo.orders(user_id, created_at DESC);
CREATE INDEX IX_orders_channel_date ON dbo.orders(channel, created_at DESC);
GO

CREATE TABLE dbo.order_items (
    id           BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_order_items PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    variant_id   INT NULL,
    product_id   INT NULL,
    product_name NVARCHAR(200) NOT NULL,
    variant_name NVARCHAR(150) NULL,
    sku          NVARCHAR(100) NULL,
    quantity     INT NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    discount     DECIMAL(12,2) NOT NULL CONSTRAINT DF_order_items_discount DEFAULT 0,
    line_total   DECIMAL(15,2) NOT NULL,
    created_at   DATETIME2 NOT NULL CONSTRAINT DF_order_items_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_oi_order FOREIGN KEY (order_id)
        REFERENCES dbo.orders(id) ON DELETE NO ACTION,
    CONSTRAINT FK_oi_variant FOREIGN KEY (variant_id)
        REFERENCES dbo.product_variants(id) ON DELETE NO ACTION,
    CONSTRAINT FK_oi_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_oi_order ON dbo.order_items(order_id);
CREATE INDEX IX_oi_variant ON dbo.order_items(variant_id);
GO

CREATE TABLE dbo.payments (
    id              BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_payments PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,
    method          NVARCHAR(50) NOT NULL,
    transaction_ref NVARCHAR(100) NULL,
    status          NVARCHAR(20) NOT NULL CONSTRAINT DF_payments_status DEFAULT N'PENDING',
    paid_at         DATETIME2 NULL,
    created_at      DATETIME2 NOT NULL CONSTRAINT DF_payments_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_payments_order FOREIGN KEY (order_id)
        REFERENCES dbo.orders(id) ON DELETE CASCADE
);
GO
CREATE INDEX IX_payments_order_date ON dbo.payments(order_id, created_at DESC);
GO

CREATE TABLE dbo.returns (
    id            BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_returns PRIMARY KEY,
    order_id      BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity      INT NOT NULL,
    reason        NVARCHAR(500) NULL,
    refund_amount DECIMAL(15,2) NOT NULL CONSTRAINT DF_returns_refund_amount DEFAULT 0,
    refund_method NVARCHAR(50) NULL,
    refund_status NVARCHAR(20) NOT NULL CONSTRAINT DF_returns_refund_status DEFAULT N'PENDING',
    refunded_at   DATETIME2 NULL,
    status        NVARCHAR(20) NOT NULL CONSTRAINT DF_returns_status DEFAULT N'PENDING',
    note          NVARCHAR(MAX) NULL,
    processed_by  INT NULL,
    created_at    DATETIME2 NOT NULL CONSTRAINT DF_returns_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT FK_returns_order FOREIGN KEY (order_id)
        REFERENCES dbo.orders(id) ON DELETE NO ACTION,
    CONSTRAINT FK_returns_item FOREIGN KEY (order_item_id)
        REFERENCES dbo.order_items(id) ON DELETE NO ACTION,
    CONSTRAINT FK_returns_user FOREIGN KEY (processed_by)
        REFERENCES dbo.users(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_returns_order ON dbo.returns(order_id, created_at DESC);
CREATE INDEX IX_returns_item ON dbo.returns(order_item_id);
GO

---------------------------------------------------------------
-- 8) SYSTEM SETTINGS
---------------------------------------------------------------
CREATE TABLE dbo.system_settings (
    setting_key   NVARCHAR(50) NOT NULL CONSTRAINT PK_system_settings PRIMARY KEY,
    setting_value NVARCHAR(100) NOT NULL,
    updated_at    DATETIME2 NOT NULL CONSTRAINT DF_system_settings_updated_at DEFAULT SYSDATETIME()
);
GO

---------------------------------------------------------------
-- 9) CART ITEMS
---------------------------------------------------------------
CREATE TABLE dbo.cart_items (
    id                 INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_cart_items PRIMARY KEY,
    customer_id         INT NOT NULL,
    product_variant_id  INT NOT NULL,
    quantity            INT NOT NULL,
    created_at          DATETIME2 NOT NULL CONSTRAINT DF_cart_items_created_at DEFAULT SYSDATETIME(),
    updated_at          DATETIME2 NOT NULL CONSTRAINT DF_cart_items_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT CK_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT UQ_cart_items_customer_variant UNIQUE (customer_id, product_variant_id),
    CONSTRAINT FK_cart_items_customer FOREIGN KEY (customer_id)
        REFERENCES dbo.customers(id) ON DELETE CASCADE,
    CONSTRAINT FK_cart_items_product_variant FOREIGN KEY (product_variant_id)
        REFERENCES dbo.product_variants(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_cart_items_customer ON dbo.cart_items(customer_id);
CREATE INDEX IX_cart_items_variant ON dbo.cart_items(product_variant_id);
GO

---------------------------------------------------------------
-- SEED (minimal)
-- (Không dùng table variable để tránh lỗi scope khi chạy theo từng batch GO)
---------------------------------------------------------------
INSERT INTO dbo.system_settings(setting_key, setting_value)
VALUES (N'DEFAULT_CURRENCY', N'VND');
GO

INSERT INTO dbo.users (username, email, password_hash, role, is_active)
VALUES
(N'admin',      N'admin@laptopstore.vn',     N'$2a$10$admin.hash.placeholder',     N'ADMIN',     1),
(N'sales01',    N'sales01@laptopstore.vn',   N'$2a$10$sales.hash.placeholder',     N'SALES',     1),
(N'inv01',      N'inv01@laptopstore.vn',     N'$2a$10$inv.hash.placeholder',       N'INVENTORY', 1),
(N'customer01', N'customer01@gmail.com',     N'$2a$10$cust1.hash.placeholder',     N'CUSTOMER',  1);
GO

INSERT INTO dbo.customers (user_id, name, email, phone, date_of_birth, customer_type, vip_tier, segments_json,
                           loyalty_points, total_spent, last_order_at, last_login_at, address, notes, is_active)
VALUES
(
    (SELECT id FROM dbo.users WHERE username = N'customer01'),
    N'Customer 01',
    N'customer01@gmail.com',
    NULL,
    NULL,
    N'REGULAR',
    NULL,
    NULL,
    0,
    0,
    NULL,
    NULL,
    NULL,
    NULL,
    1
);
GO

---------------------------------------------------------------
-- 0) BỔ SUNG USERS + CUSTOMERS (để có >=10 customers)
---------------------------------------------------------------
;WITH n AS (
    SELECT TOP (9) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) + 1 AS i
    FROM sys.objects
)
INSERT INTO dbo.users (username, email, password_hash, role, is_active)
SELECT
    CONCAT(N'customer', RIGHT(N'00' + CONVERT(NVARCHAR(10), i), 2)) AS username,
    CONCAT(N'customer', RIGHT(N'00' + CONVERT(NVARCHAR(10), i), 2), N'@gmail.com') AS email,
    N'$2a$10$cust.hash.placeholder' AS password_hash,
    N'CUSTOMER' AS role,
    1
FROM n
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.users u
    WHERE u.username = CONCAT(N'customer', RIGHT(N'00' + CONVERT(NVARCHAR(10), i), 2))
);
GO

;WITH n AS (
    SELECT TOP (9) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) + 1 AS i
    FROM sys.objects
)
INSERT INTO dbo.customers (
    user_id, name, email, phone, date_of_birth, customer_type, vip_tier, segments_json,
    loyalty_points, total_spent, last_order_at, last_login_at, address, notes, is_active
)
SELECT
    u.id,
    CONCAT(N'Customer ', RIGHT(N'00' + CONVERT(NVARCHAR(10), n.i), 2)) AS name,
    u.email,
    CONCAT(N'09', RIGHT(N'00000000' + CONVERT(NVARCHAR(10), 10000000 + n.i * 12345), 8)) AS phone,
    DATEADD(DAY, -1*(3650 + n.i*111), CAST(GETDATE() AS DATE)) AS date_of_birth,
    CASE WHEN n.i IN (8,9,10) THEN N'VIP' ELSE N'REGULAR' END AS customer_type,
    CASE WHEN n.i IN (8,9,10) THEN N'GOLD' ELSE NULL END AS vip_tier,
    CASE WHEN n.i IN (8,9,10) THEN N'{"segment":"high_value","channel":"online"}' ELSE N'{"segment":"regular","channel":"offline"}' END,
    CASE WHEN n.i IN (8,9,10) THEN 500 + n.i*20 ELSE 50 + n.i*10 END AS loyalty_points,
    CASE WHEN n.i IN (8,9,10) THEN 25000000 + n.i*1000000 ELSE 1500000 + n.i*200000 END AS total_spent,
    NULL, NULL,
    CONCAT(N'Hà Nội, Q.', n.i, N', P.', n.i) AS address,
    NULL,
    1
FROM n
JOIN dbo.users u
  ON u.username = CONCAT(N'customer', RIGHT(N'00' + CONVERT(NVARCHAR(10), n.i), 2))
WHERE NOT EXISTS (SELECT 1 FROM dbo.customers c WHERE c.user_id = u.id);
GO

---------------------------------------------------------------
-- 1) USER_LOGINS (>=10)
---------------------------------------------------------------
DECLARE @custUserIds TABLE(id INT);
INSERT INTO @custUserIds(id)
SELECT TOP (10) id FROM dbo.users WHERE role = N'CUSTOMER' ORDER BY id;

INSERT INTO dbo.user_logins (user_id, username, success, ip_address, user_agent, created_at, updated_at)
SELECT
    u.id,
    (SELECT username FROM dbo.users WHERE id = u.id),
    CASE WHEN ROW_NUMBER() OVER (ORDER BY u.id) % 5 = 0 THEN 0 ELSE 1 END,
    CONCAT(N'192.168.1.', ROW_NUMBER() OVER (ORDER BY u.id) + 10),
    N'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
    DATEADD(HOUR, -1*(ROW_NUMBER() OVER (ORDER BY u.id) * 6), SYSDATETIME()),
    SYSDATETIME()
FROM @custUserIds u;
GO

---------------------------------------------------------------
-- 2) PASSWORD_RESETS (>=10)
---------------------------------------------------------------
INSERT INTO dbo.password_resets (user_id, token_hash, expires_at, used_at, requested_ip, created_at)
SELECT
    u.id,
    CONCAT(N'tokenhash_', u.id, N'_', CONVERT(NVARCHAR(30), ABS(CHECKSUM(NEWID())))),
    DATEADD(HOUR, 2, SYSDATETIME()),
    CASE WHEN ROW_NUMBER() OVER (ORDER BY u.id) % 3 = 0 THEN DATEADD(MINUTE, 10, SYSDATETIME()) ELSE NULL END,
    CONCAT(N'10.0.0.', ROW_NUMBER() OVER (ORDER BY u.id) + 20),
    DATEADD(DAY, -1*(ROW_NUMBER() OVER (ORDER BY u.id)), SYSDATETIME())
FROM (SELECT TOP (10) id FROM dbo.users ORDER BY id) u;
GO

---------------------------------------------------------------
-- 3) AUDIT_LOGS (>=10)
---------------------------------------------------------------
DECLARE @adminId INT = (SELECT id FROM dbo.users WHERE username = N'admin');
DECLARE @salesId INT = (SELECT id FROM dbo.users WHERE username = N'sales01');
DECLARE @invId   INT = (SELECT id FROM dbo.users WHERE username = N'inv01');

INSERT INTO dbo.audit_logs (user_id, module, action, target_type, target_id, details_json, ip_address, created_at)
VALUES
(@adminId, N'AUTH',      N'LOGIN',     N'users',     1,  N'{"result":"success"}', N'203.0.113.10', DATEADD(HOUR,-2,SYSDATETIME())),
(@salesId, N'ORDERS',    N'CREATE',    N'orders',    1000, N'{"channel":"OFFLINE"}', N'203.0.113.11', DATEADD(HOUR,-5,SYSDATETIME())),
(@salesId, N'ORDERS',    N'PAYMENT',   N'payments',  1,  N'{"method":"CASH"}', N'203.0.113.11', DATEADD(HOUR,-4,SYSDATETIME())),
(@invId,   N'INVENTORY', N'STOCK_IN',  N'variants',  1,  N'{"qty":50,"ref":"PO-0001"}', N'203.0.113.12', DATEADD(DAY,-1,SYSDATETIME())),
(@invId,   N'INVENTORY', N'STOCK_OUT', N'variants',  2,  N'{"qty":2,"ref":"SO-0001"}', N'203.0.113.12', DATEADD(DAY,-2,SYSDATETIME())),
(@adminId, N'SYSTEM',    N'UPDATE',    N'settings',  0,  N'{"key":"DEFAULT_CURRENCY","value":"VND"}', N'203.0.113.10', DATEADD(DAY,-3,SYSDATETIME())),
(@salesId, N'CUSTOMERS', N'UPDATE',    N'customers', 1,  N'{"field":"loyalty_points","delta":100}', N'203.0.113.11', DATEADD(DAY,-4,SYSDATETIME())),
(@salesId, N'PROMO',     N'CREATE',    N'promotions',1,  N'{"code":"TET10"}', N'203.0.113.11', DATEADD(DAY,-5,SYSDATETIME())),
(@invId,   N'PRODUCTS',  N'CREATE',    N'products',  1,  N'{"sku":"LAP-DELL-001"}', N'203.0.113.12', DATEADD(DAY,-6,SYSDATETIME())),
(@adminId, N'AUTH',      N'LOGOUT',    NULL,         NULL, N'{"reason":"user_action"}', N'203.0.113.10', DATEADD(HOUR,-1,SYSDATETIME()));
GO

---------------------------------------------------------------
-- 4) CATEGORIES (>=10)
---------------------------------------------------------------
INSERT INTO dbo.categories (name, description, image_url, parent_id, display_order, is_active)
VALUES
(N'Laptop', N'Laptop các hãng', NULL, NULL, 1, 1),
(N'Gaming Laptop', N'Laptop gaming', NULL, 1, 2, 1),
(N'Ultrabook', N'Laptop mỏng nhẹ', NULL, 1, 3, 1),
(N'Workstation', N'Laptop đồ hoạ/kỹ thuật', NULL, 1, 4, 1),
(N'Phụ kiện', N'Phụ kiện laptop', NULL, NULL, 5, 1),
(N'Chuột', N'Chuột gaming/văn phòng', NULL, 5, 6, 1),
(N'Bàn phím', N'Bàn phím cơ/office', NULL, 5, 7, 1),
(N'Màn hình', N'Monitor', NULL, NULL, 8, 1),
(N'RAM', N'Nâng cấp RAM', NULL, 5, 9, 1),
(N'SSD', N'Nâng cấp SSD', NULL, 5, 10, 1);
GO

---------------------------------------------------------------
-- 5) TAGS (>=10)
---------------------------------------------------------------
INSERT INTO dbo.tags (name, tag_type, is_active)
VALUES
(N'New', N'GENERAL', 1),
(N'Hot', N'GENERAL', 1),
(N'BestSeller', N'GENERAL', 1),
(N'Office', N'USE_CASE', 1),
(N'Gaming', N'USE_CASE', 1),
(N'Design', N'USE_CASE', 1),
(N'Intel', N'BRAND', 1),
(N'AMD', N'BRAND', 1),
(N'RTX', N'FEATURE', 1),
(N'OLED', N'FEATURE', 1);
GO

---------------------------------------------------------------
-- 6) PRODUCTS (>=10) + VARIANTS (>=10)
---------------------------------------------------------------
INSERT INTO dbo.products (name, sku, description, is_visible, is_featured, attributes_json)
VALUES
(N'Dell Inspiron 15 3530',        N'LAP-DELL-001', N'Laptop phổ thông học tập - văn phòng', 1, 0, N'{"brand":"Dell","series":"Inspiron"}'),
(N'HP Pavilion 14',               N'LAP-HP-001',   N'Laptop mỏng nhẹ cho văn phòng',       1, 0, N'{"brand":"HP","series":"Pavilion"}'),
(N'Lenovo IdeaPad 5',             N'LAP-LEN-001',  N'Hiệu năng tốt trong tầm giá',         1, 1, N'{"brand":"Lenovo","series":"IdeaPad"}'),
(N'ASUS TUF Gaming F15',          N'LAP-ASUS-001', N'Laptop gaming phổ thông',             1, 1, N'{"brand":"ASUS","series":"TUF"}'),
(N'Acer Nitro 5',                 N'LAP-ACER-001', N'Gaming Nitro',                         1, 0, N'{"brand":"Acer","series":"Nitro"}'),
(N'MSI Katana 15',                N'LAP-MSI-001',  N'Gaming hiệu năng cao',                1, 0, N'{"brand":"MSI","series":"Katana"}'),
(N'Dell XPS 13',                  N'LAP-DELL-002', N'Ultrabook cao cấp',                   1, 1, N'{"brand":"Dell","series":"XPS"}'),
(N'LG UltraFine 27" 4K',          N'MON-LG-001',   N'Màn hình 4K',                          1, 0, N'{"brand":"LG","type":"Monitor"}'),
(N'Kingston RAM DDR4 16GB',       N'RAM-KING-016', N'RAM DDR4 16GB',                       1, 0, N'{"brand":"Kingston","type":"RAM"}'),
(N'Samsung SSD 980 1TB',          N'SSD-SAM-1TB',  N'SSD NVMe 1TB',                        1, 1, N'{"brand":"Samsung","type":"SSD"}');
GO

-- Gắn category cho product (>=10 rows)
INSERT INTO dbo.product_categories (product_id, category_id, is_primary)
SELECT p.id,
       CASE
         WHEN p.sku LIKE N'LAP-%' AND p.sku IN (N'LAP-ASUS-001',N'LAP-ACER-001',N'LAP-MSI-001') THEN (SELECT id FROM dbo.categories WHERE name=N'Gaming Laptop')
         WHEN p.sku LIKE N'LAP-%' AND p.sku = N'LAP-DELL-002' THEN (SELECT id FROM dbo.categories WHERE name=N'Ultrabook')
         WHEN p.sku LIKE N'LAP-%' THEN (SELECT id FROM dbo.categories WHERE name=N'Laptop')
         WHEN p.sku LIKE N'MON-%' THEN (SELECT id FROM dbo.categories WHERE name=N'Màn hình')
         WHEN p.sku LIKE N'RAM-%' THEN (SELECT id FROM dbo.categories WHERE name=N'RAM')
         WHEN p.sku LIKE N'SSD-%' THEN (SELECT id FROM dbo.categories WHERE name=N'SSD')
         ELSE (SELECT TOP 1 id FROM dbo.categories ORDER BY id)
       END AS category_id,
       1
FROM dbo.products p
WHERE NOT EXISTS (SELECT 1 FROM dbo.product_categories pc WHERE pc.product_id = p.id);
GO

-- VARIANTS (mỗi product 1-2 variants => đảm bảo >=10)
INSERT INTO dbo.product_variants (
    product_id, variant_name, sku, barcode, currency_code, price, cost_price,
    price_tiers_json, stock_quantity, reserved_qty, attributes_json, is_active
)
SELECT p.id,
       CASE
         WHEN p.sku LIKE N'LAP-%' THEN N'Core i5 / 16GB / 512GB'
         WHEN p.sku LIKE N'MON-%' THEN N'27 inch / 4K'
         WHEN p.sku LIKE N'RAM-%' THEN N'16GB DDR4 3200'
         WHEN p.sku LIKE N'SSD-%' THEN N'NVMe 1TB'
         ELSE N'Default'
       END,
       CONCAT(p.sku, N'-V1'),
       CONCAT(N'BC', RIGHT(N'0000000000' + CONVERT(NVARCHAR(10), ABS(CHECKSUM(NEWID()))), 10)),
       'VND',
       CASE
         WHEN p.sku = N'LAP-DELL-001' THEN 15990000
         WHEN p.sku = N'LAP-HP-001'   THEN 16990000
         WHEN p.sku = N'LAP-LEN-001'  THEN 17990000
         WHEN p.sku = N'LAP-ASUS-001' THEN 21990000
         WHEN p.sku = N'LAP-ACER-001' THEN 22990000
         WHEN p.sku = N'LAP-MSI-001'  THEN 26990000
         WHEN p.sku = N'LAP-DELL-002' THEN 39990000
         WHEN p.sku = N'MON-LG-001'   THEN 10990000
         WHEN p.sku = N'RAM-KING-016' THEN 899000
         WHEN p.sku = N'SSD-SAM-1TB'  THEN 1999000
         ELSE 1000000
       END,
       NULL,
       NULL,
       CASE
         WHEN p.sku LIKE N'LAP-%' THEN 25
         WHEN p.sku LIKE N'MON-%' THEN 20
         ELSE 60
       END,
       0,
       p.attributes_json,
       1
FROM dbo.products p
WHERE NOT EXISTS (SELECT 1 FROM dbo.product_variants v WHERE v.sku = CONCAT(p.sku, N'-V1'));
GO

-- Thêm 1 variant V2 cho 5 laptop để dữ liệu “thật” hơn
INSERT INTO dbo.product_variants (
    product_id, variant_name, sku, barcode, currency_code, price, cost_price,
    price_tiers_json, stock_quantity, reserved_qty, attributes_json, is_active
)
SELECT TOP (5) p.id,
       N'Core i7 / 16GB / 1TB',
       CONCAT(p.sku, N'-V2'),
       CONCAT(N'BC', RIGHT(N'0000000000' + CONVERT(NVARCHAR(10), ABS(CHECKSUM(NEWID()))), 10)),
       'VND',
       CASE
         WHEN p.sku = N'LAP-ASUS-001' THEN 26990000
         WHEN p.sku = N'LAP-ACER-001' THEN 27990000
         WHEN p.sku = N'LAP-MSI-001'  THEN 32990000
         WHEN p.sku = N'LAP-DELL-002' THEN 45990000
         ELSE 20990000
       END,
       NULL, NULL,
       10, 0,
       p.attributes_json,
       1
FROM dbo.products p
WHERE p.sku LIKE N'LAP-%'
  AND NOT EXISTS (SELECT 1 FROM dbo.product_variants v WHERE v.sku = CONCAT(p.sku, N'-V2'));
GO

---------------------------------------------------------------
-- 7) PRODUCT_TAGS (>=10)
---------------------------------------------------------------
;WITH p AS (
    SELECT TOP (10) id, sku FROM dbo.products ORDER BY id
),
t AS (
    SELECT id, name FROM dbo.tags
)
INSERT INTO dbo.product_tags(product_id, tag_id)
SELECT p.id,
       CASE
         WHEN p.sku IN (N'LAP-ASUS-001',N'LAP-ACER-001',N'LAP-MSI-001') THEN (SELECT id FROM dbo.tags WHERE name=N'Gaming')
         WHEN p.sku IN (N'LAP-DELL-002') THEN (SELECT id FROM dbo.tags WHERE name=N'Hot')
         WHEN p.sku IN (N'SSD-SAM-1TB') THEN (SELECT id FROM dbo.tags WHERE name=N'BestSeller')
         ELSE (SELECT id FROM dbo.tags WHERE name=N'New')
       END
FROM p
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.product_tags pt
    WHERE pt.product_id = p.id
      AND pt.tag_id = CASE
         WHEN p.sku IN (N'LAP-ASUS-001',N'LAP-ACER-001',N'LAP-MSI-001') THEN (SELECT id FROM dbo.tags WHERE name=N'Gaming')
         WHEN p.sku IN (N'LAP-DELL-002') THEN (SELECT id FROM dbo.tags WHERE name=N'Hot')
         WHEN p.sku IN (N'SSD-SAM-1TB') THEN (SELECT id FROM dbo.tags WHERE name=N'BestSeller')
         ELSE (SELECT id FROM dbo.tags WHERE name=N'New')
       END
);
GO

-- Thêm tag phụ để tổng rows chắc chắn >=10
INSERT INTO dbo.product_tags(product_id, tag_id)
SELECT TOP (10) p.id, (SELECT id FROM dbo.tags WHERE name=N'Intel')
FROM dbo.products p
WHERE p.sku LIKE N'LAP-%'
  AND NOT EXISTS (SELECT 1 FROM dbo.product_tags pt WHERE pt.product_id = p.id AND pt.tag_id = (SELECT id FROM dbo.tags WHERE name=N'Intel'));
GO

---------------------------------------------------------------
-- 8) IMAGES (>=10) (đúng CHECK owner: hoặc product_id hoặc variant_id)
---------------------------------------------------------------
INSERT INTO dbo.images (product_id, variant_id, url, is_primary, sort_order, alt_text, meta_json)
SELECT TOP (10)
    p.id,
    NULL,
    CONCAT(N'https://cdn.laptopstore.vn/products/', p.sku, N'/main.jpg'),
    1,
    0,
    CONCAT(p.name, N' main image'),
    N'{"source":"seed"}'
FROM dbo.products p
WHERE NOT EXISTS (SELECT 1 FROM dbo.images i WHERE i.product_id = p.id AND i.is_primary = 1)
ORDER BY p.id;
GO

INSERT INTO dbo.images (product_id, variant_id, url, is_primary, sort_order, alt_text, meta_json)
SELECT TOP (10)
    NULL,
    v.id,
    CONCAT(N'https://cdn.laptopstore.vn/variants/', v.sku, N'/1.jpg'),
    0,
    1,
    CONCAT(v.sku, N' image 1'),
    N'{"source":"seed"}'
FROM dbo.product_variants v
WHERE NOT EXISTS (SELECT 1 FROM dbo.images i WHERE i.variant_id = v.id)
ORDER BY v.id;
GO

---------------------------------------------------------------
-- 9) STOCK_TRANSACTIONS (>=10)
---------------------------------------------------------------
INSERT INTO dbo.stock_transactions (variant_id, quantity, type, reference_type, reference_id, note, created_by, created_at)
SELECT TOP (10)
    v.id,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY v.id) % 2 = 0 THEN 20 ELSE -2 END AS quantity,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY v.id) % 2 = 0 THEN N'STOCK_IN' ELSE N'STOCK_OUT' END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY v.id) % 2 = 0 THEN N'PURCHASE_ORDER' ELSE N'SALES_ORDER' END,
    NULL,
    N'Seed inventory movement',
    @invId,
    DATEADD(DAY, -1*(ROW_NUMBER() OVER (ORDER BY v.id)), SYSDATETIME())
FROM dbo.product_variants v
ORDER BY v.id;
GO

---------------------------------------------------------------
-- 10) PROMOTIONS (>=10)
---------------------------------------------------------------
INSERT INTO dbo.promotions (
    code, name, description, discount_type, discount_value,
    min_order_amount, applicability_json, rules_json, priority, stackable,
    start_date, end_date, usage_limit, used_count, is_active, created_by
)
VALUES
(N'TET10',  N'Tết giảm 10%',     N'Giảm 10% đơn hàng', N'PERCENT', 10,  1000000, N'{"scope":"all"}', N'{"max_discount":1000000}', 10, 0, DATEADD(DAY,-30,SYSDATETIME()), DATEADD(DAY, 30,SYSDATETIME()), 500,  23, 1, @adminId),
(N'WELCOME',N'Chào mừng',        N'Giảm 100k cho đơn đầu', N'AMOUNT', 100000, 500000, N'{"first_order":true}', N'{}', 9, 0, DATEADD(DAY,-60,SYSDATETIME()), DATEADD(DAY, 60,SYSDATETIME()), 2000, 180, 1, @adminId),
(N'FREESHIP',N'Miễn phí ship',   N'Free ship tối đa 50k', N'AMOUNT', 50000,  300000, N'{"shipping_only":true}', N'{}', 8, 1, DATEADD(DAY,-10,SYSDATETIME()), DATEADD(DAY, 90,SYSDATETIME()), NULL, 420, 1, @salesId),
(N'WEEKEND5',N'Cuối tuần 5%',    N'Giảm 5% cuối tuần', N'PERCENT', 5, NULL, N'{"days":["sat","sun"]}', N'{}', 7, 0, DATEADD(DAY,-14,SYSDATETIME()), DATEADD(DAY, 14,SYSDATETIME()), 300,  40, 1, @salesId),
(N'VIP200', N'VIP giảm 200k',    N'Chỉ VIP', N'AMOUNT', 200000, 2000000, N'{"segment":"vip"}', N'{}', 6, 0, DATEADD(DAY,-90,SYSDATETIME()), DATEADD(DAY, 30,SYSDATETIME()), 1000, 77, 1, @adminId),
(N'RAM50',  N'RAM giảm 50k',     N'Áp dụng cho RAM', N'AMOUNT', 50000, NULL, N'{"category":"RAM"}', N'{}', 5, 1, DATEADD(DAY,-20,SYSDATETIME()), DATEADD(DAY, 40,SYSDATETIME()), 400,  12, 1, @invId),
(N'SSD3',   N'SSD giảm 3%',      N'Áp dụng cho SSD', N'PERCENT', 3, NULL, N'{"category":"SSD"}', N'{}', 4, 1, DATEADD(DAY,-20,SYSDATETIME()), DATEADD(DAY, 40,SYSDATETIME()), 400,  9, 1, @invId),
(N'FLASH15',N'Flash sale 15%',   N'Giảm 15% khung giờ', N'PERCENT', 15,  2000000, N'{"timebox":"20:00-22:00"}', N'{"max_discount":1500000}', 20, 0, DATEADD(DAY,-2,SYSDATETIME()), DATEADD(DAY, 2,SYSDATETIME()), 200,  60, 1, @adminId),
(N'STU100', N'Sinh viên 100k',   N'Giảm 100k cho SV', N'AMOUNT', 100000, 700000, N'{"segment":"student"}', N'{}', 3, 0, DATEADD(DAY,-120,SYSDATETIME()), DATEADD(DAY,120,SYSDATETIME()), 800,  55, 1, @salesId),
(N'OLDSTOCK',N'Xả kho',          N'Giảm giá xả kho', N'PERCENT', 8, NULL, N'{"clearance":true}', N'{}', 2, 1, DATEADD(DAY,-200,SYSDATETIME()), DATEADD(DAY, 10,SYSDATETIME()), NULL, 300, 1, @invId);
GO

---------------------------------------------------------------
-- 11) PRICE_HISTORY (>=10)
---------------------------------------------------------------
INSERT INTO dbo.price_history (variant_id, currency_code, price, cost_price, reason, effective_from, effective_to, created_by)
SELECT TOP (10)
    v.id,
    'VND',
    v.price,
    v.cost_price,
    N'Initial price seed',
    DATEADD(DAY, -60, SYSDATETIME()),
    NULL,
    @adminId
FROM dbo.product_variants v
ORDER BY v.id;
GO

---------------------------------------------------------------
-- 12) LOYALTY_LEDGER (>=10)
---------------------------------------------------------------
INSERT INTO dbo.loyalty_ledger (customer_id, points_delta, reason, reference_type, reference_id, note, created_by, created_at)
SELECT TOP (10)
    c.id,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY c.id) % 2 = 0 THEN 100 ELSE 50 END,
    N'Purchase reward',
    N'ORDER',
    NULL,
    N'Seed loyalty points',
    @salesId,
    DATEADD(DAY, -1*(ROW_NUMBER() OVER (ORDER BY c.id)), SYSDATETIME())
FROM dbo.customers c
ORDER BY c.id;
GO

---------------------------------------------------------------
-- 13) ORDERS (>=10)
---------------------------------------------------------------
DECLARE @promoWelcome NVARCHAR(50) = N'WELCOME';

;WITH cust AS (
    SELECT TOP (10) c.id AS customer_id, c.user_id
    FROM dbo.customers c
    ORDER BY c.id
)
INSERT INTO dbo.orders (
    customer_id, user_id, channel, status, payment_method, payment_status,
    shipping_address, shipping_fee, shipping_status, tracking_code,
    paid_at, shipped_at, delivered_at, cancelled_at, returned_at,
    subtotal, discount_total, tax_total, total_amount,
    applied_promotion_code, applied_promotion_json, notes
)
SELECT
    cust.customer_id,
    @salesId,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 2 = 0 THEN N'ONLINE' ELSE N'OFFLINE' END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 5 = 0 THEN N'DELIVERED'
         WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 5 = 1 THEN N'PAID'
         ELSE N'PENDING' END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 2 = 0 THEN N'BANK_TRANSFER' ELSE N'CASH' END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 3 = 0 THEN N'PAID' ELSE N'UNPAID' END,
    (SELECT address FROM dbo.customers WHERE id = cust.customer_id),
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 2 = 0 THEN 30000 ELSE 0 END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 4 = 0 THEN N'SHIPPED' ELSE NULL END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 4 = 0 THEN CONCAT(N'VN', RIGHT(N'000000' + CONVERT(NVARCHAR(10), cust.customer_id), 6)) ELSE NULL END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 3 = 0 THEN DATEADD(DAY, -2, SYSDATETIME()) ELSE NULL END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 4 = 0 THEN DATEADD(DAY, -1, SYSDATETIME()) ELSE NULL END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 5 = 0 THEN DATEADD(DAY,  0, SYSDATETIME()) ELSE NULL END,
    NULL, NULL,
    0, 0, 0, 0,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 2 = 0 THEN @promoWelcome ELSE NULL END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY cust.customer_id) % 2 = 0 THEN N'{"code":"WELCOME","amount":100000}' ELSE NULL END,
    N'Seed order'
FROM cust;
GO

---------------------------------------------------------------
-- 14) ORDER_ITEMS (>=10) + cập nhật totals trong ORDERS cho “thật”
---------------------------------------------------------------
-- Mỗi order 1 item (>=10)
;WITH o AS (
    SELECT TOP (10) id AS order_id FROM dbo.orders ORDER BY id DESC
),
v AS (
    SELECT TOP (10) v.id AS variant_id, v.product_id, v.sku AS variant_sku, v.variant_name, v.price,
           p.name AS product_name, p.sku AS product_sku
    FROM dbo.product_variants v
    JOIN dbo.products p ON p.id = v.product_id
    ORDER BY v.id
)
INSERT INTO dbo.order_items (
    order_id, variant_id, product_id, product_name, variant_name, sku,
    quantity, unit_price, discount, line_total
)
SELECT
    o.order_id,
    v.variant_id,
    v.product_id,
    v.product_name,
    v.variant_name,
    v.variant_sku,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY o.order_id) % 3 = 0 THEN 2 ELSE 1 END,
    v.price,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY o.order_id) % 2 = 0 THEN 50000 ELSE 0 END,
    (CASE WHEN ROW_NUMBER() OVER (ORDER BY o.order_id) % 3 = 0 THEN 2 ELSE 1 END) * (v.price - (CASE WHEN ROW_NUMBER() OVER (ORDER BY o.order_id) % 2 = 0 THEN 50000 ELSE 0 END))
FROM o
JOIN v ON v.variant_id = (SELECT TOP 1 variant_id FROM v ORDER BY v.variant_id OFFSET (ROW_NUMBER() OVER (ORDER BY o.order_id)-1) ROWS);
GO

-- Cập nhật subtotal/discount/tax/total cho 10 orders mới nhất
;WITH x AS (
    SELECT oi.order_id,
           SUM(oi.quantity * oi.unit_price) AS subtotal,
           SUM(oi.quantity * oi.discount)   AS discount_total
    FROM dbo.order_items oi
    WHERE oi.order_id IN (SELECT TOP (10) id FROM dbo.orders ORDER BY id DESC)
    GROUP BY oi.order_id
)
UPDATE o
SET
    o.subtotal       = x.subtotal,
    o.discount_total = x.discount_total,
    o.tax_total      = CAST(0 AS DECIMAL(15,2)),
    o.total_amount   = x.subtotal - x.discount_total + o.shipping_fee,
    o.updated_at     = SYSDATETIME()
FROM dbo.orders o
JOIN x ON x.order_id = o.id;
GO

---------------------------------------------------------------
-- 15) PAYMENTS (>=10)
---------------------------------------------------------------
INSERT INTO dbo.payments (order_id, amount, method, transaction_ref, status, paid_at)
SELECT TOP (10)
    o.id,
    o.total_amount,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY o.id) % 2 = 0 THEN N'BANK_TRANSFER' ELSE N'CASH' END,
    CONCAT(N'TXN-', CONVERT(NVARCHAR(20), o.id)),
    CASE WHEN o.payment_status = N'PAID' THEN N'SUCCESS' ELSE N'PENDING' END,
    CASE WHEN o.payment_status = N'PAID' THEN ISNULL(o.paid_at, DATEADD(DAY,-1,SYSDATETIME())) ELSE NULL END
FROM dbo.orders o
ORDER BY o.id DESC;
GO

---------------------------------------------------------------
-- 16) RETURNS (>=10)
---------------------------------------------------------------
-- Tạo 10 returns dựa trên 10 order_items gần nhất
INSERT INTO dbo.returns (
    order_id, order_item_id, quantity, reason,
    refund_amount, refund_method, refund_status, refunded_at,
    status, note, processed_by
)
SELECT TOP (10)
    oi.order_id,
    oi.id,
    1,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY oi.id) % 2 = 0 THEN N'Lỗi kỹ thuật / đổi trả' ELSE N'Không vừa nhu cầu' END,
    oi.unit_price - oi.discount,
    N'BANK_TRANSFER',
    CASE WHEN ROW_NUMBER() OVER (ORDER BY oi.id) % 3 = 0 THEN N'REFUNDED' ELSE N'PENDING' END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY oi.id) % 3 = 0 THEN DATEADD(DAY,-1,SYSDATETIME()) ELSE NULL END,
    N'PENDING',
    N'Seed return request',
    @salesId
FROM dbo.order_items oi
ORDER BY oi.id DESC;
GO

---------------------------------------------------------------
-- 17) CART_ITEMS (>=10)
---------------------------------------------------------------
;WITH c AS (
    SELECT TOP (10) id AS customer_id FROM dbo.customers ORDER BY id
),
v AS (
    SELECT TOP (10) id AS variant_id FROM dbo.product_variants ORDER BY id
),
cv AS (
    SELECT c.customer_id, v.variant_id,
           ROW_NUMBER() OVER (ORDER BY c.customer_id) AS rn
    FROM c
    CROSS JOIN v
)
INSERT INTO dbo.cart_items (customer_id, product_variant_id, quantity)
SELECT TOP (10)
    customer_id,
    variant_id,
    CASE WHEN rn % 3 = 0 THEN 2 ELSE 1 END
FROM cv
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.cart_items ci
    WHERE ci.customer_id = cv.customer_id AND ci.product_variant_id = cv.variant_id
)
ORDER BY rn;
GO

SET NOCOUNT ON;

---------------------------------------------------------------
-- RE-DECLARE VARIABLES (must be in the same batch; no GO in-between)
---------------------------------------------------------------
DECLARE @adminId INT = (SELECT TOP 1 id FROM dbo.users WHERE username = N'admin');
DECLARE @salesId INT = (SELECT TOP 1 id FROM dbo.users WHERE username = N'sales01');
DECLARE @invId   INT = (SELECT TOP 1 id FROM dbo.users WHERE username = N'inv01');

---------------------------------------------------------------
-- 9) STOCK_TRANSACTIONS (ensure >= 10 seed rows)
---------------------------------------------------------------
;WITH v AS (
    SELECT TOP (10) id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM dbo.product_variants
)
INSERT INTO dbo.stock_transactions (variant_id, quantity, type, reference_type, reference_id, note, created_by, created_at)
SELECT
    v.id,
    CASE WHEN v.rn % 2 = 0 THEN 20 ELSE -2 END,
    CASE WHEN v.rn % 2 = 0 THEN N'STOCK_IN' ELSE N'STOCK_OUT' END,
    CASE WHEN v.rn % 2 = 0 THEN N'PURCHASE_ORDER' ELSE N'SALES_ORDER' END,
    NULL,
    N'Seed inventory movement',
    @invId,
    DATEADD(DAY, -1*v.rn, SYSDATETIME())
FROM v
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.stock_transactions st
    WHERE st.variant_id = v.id
      AND st.note = N'Seed inventory movement'
);
---------------------------------------------------------------
-- 10) PROMOTIONS (ensure 10 codes exist; skip if already exists)
---------------------------------------------------------------
INSERT INTO dbo.promotions (
    code, name, description, discount_type, discount_value,
    min_order_amount, applicability_json, rules_json, priority, stackable,
    start_date, end_date, usage_limit, used_count, is_active, created_by
)
SELECT x.*
FROM (VALUES
 (N'TET10',    N'Tết giảm 10%',   N'Giảm 10% đơn hàng',      N'PERCENT', CAST(10  AS DECIMAL(12,2)), CAST(1000000 AS DECIMAL(15,2)), N'{"scope":"all"}',              N'{"max_discount":1000000}', 10, 0, DATEADD(DAY,-30,SYSDATETIME()), DATEADD(DAY, 30,SYSDATETIME()), 500,  23, 1, @adminId),
 (N'WELCOME',  N'Chào mừng',      N'Giảm 100k đơn đầu',      N'AMOUNT',  CAST(100000 AS DECIMAL(12,2)), CAST(500000 AS DECIMAL(15,2)), N'{"first_order":true}',        N'{}',                      9,  0, DATEADD(DAY,-60,SYSDATETIME()), DATEADD(DAY, 60,SYSDATETIME()), 2000, 180, 1, @adminId),
 (N'FREESHIP', N'Miễn phí ship',  N'Free ship tối đa 50k',   N'AMOUNT',  CAST(50000 AS DECIMAL(12,2)),  CAST(300000 AS DECIMAL(15,2)), N'{"shipping_only":true}',      N'{}',                      8,  1, DATEADD(DAY,-10,SYSDATETIME()), DATEADD(DAY, 90,SYSDATETIME()), NULL, 420, 1, @salesId),
 (N'WEEKEND5', N'Cuối tuần 5%',   N'Giảm 5% cuối tuần',      N'PERCENT', CAST(5   AS DECIMAL(12,2)),   NULL,                        N'{"days":["sat","sun"]}',      N'{}',                      7,  0, DATEADD(DAY,-14,SYSDATETIME()), DATEADD(DAY, 14,SYSDATETIME()), 300,  40, 1, @salesId),
 (N'VIP200',   N'VIP giảm 200k',  N'Chỉ áp dụng VIP',        N'AMOUNT',  CAST(200000 AS DECIMAL(12,2)), CAST(2000000 AS DECIMAL(15,2)), N'{"segment":"vip"}',          N'{}',                      6,  0, DATEADD(DAY,-90,SYSDATETIME()), DATEADD(DAY, 30,SYSDATETIME()), 1000, 77, 1, @adminId),
 (N'RAM50',    N'RAM giảm 50k',   N'Áp dụng cho RAM',        N'AMOUNT',  CAST(50000 AS DECIMAL(12,2)),  NULL,                        N'{"category":"RAM"}',         N'{}',                      5,  1, DATEADD(DAY,-20,SYSDATETIME()), DATEADD(DAY, 40,SYSDATETIME()), 400,  12, 1, @invId),
 (N'SSD3',     N'SSD giảm 3%',    N'Áp dụng cho SSD',        N'PERCENT', CAST(3   AS DECIMAL(12,2)),   NULL,                        N'{"category":"SSD"}',         N'{}',                      4,  1, DATEADD(DAY,-20,SYSDATETIME()), DATEADD(DAY, 40,SYSDATETIME()), 400,  9,  1, @invId),
 (N'FLASH15',  N'Flash sale 15%', N'Giảm 15% khung giờ',     N'PERCENT', CAST(15  AS DECIMAL(12,2)),  CAST(2000000 AS DECIMAL(15,2)), N'{"timebox":"20:00-22:00"}', N'{"max_discount":1500000}',20, 0, DATEADD(DAY,-2,SYSDATETIME()),  DATEADD(DAY, 2,SYSDATETIME()), 200,  60, 1, @adminId),
 (N'STU100',   N'Sinh viên 100k', N'Giảm 100k cho SV',       N'AMOUNT',  CAST(100000 AS DECIMAL(12,2)), CAST(700000 AS DECIMAL(15,2)), N'{"segment":"student"}',      N'{}',                      3,  0, DATEADD(DAY,-120,SYSDATETIME()),DATEADD(DAY,120,SYSDATETIME()), 800,  55, 1, @salesId),
 (N'OLDSTOCK', N'Xả kho',         N'Giảm giá xả kho',        N'PERCENT', CAST(8   AS DECIMAL(12,2)),   NULL,                        N'{"clearance":true}',         N'{}',                      2,  1, DATEADD(DAY,-200,SYSDATETIME()),DATEADD(DAY, 10,SYSDATETIME()), NULL, 300, 1, @invId)
) AS x(
    code, name, description, discount_type, discount_value,
    min_order_amount, applicability_json, rules_json, priority, stackable,
    start_date, end_date, usage_limit, used_count, is_active, created_by
)
WHERE NOT EXISTS (SELECT 1 FROM dbo.promotions p WHERE p.code = x.code);

---------------------------------------------------------------
-- 11) PRICE_HISTORY (ensure >=10 for first variants)
---------------------------------------------------------------
;WITH v AS (
    SELECT TOP (10) id, price, cost_price, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM dbo.product_variants
)
INSERT INTO dbo.price_history (variant_id, currency_code, price, cost_price, reason, effective_from, effective_to, created_by)
SELECT
    v.id, 'VND', v.price, v.cost_price, N'Initial price seed',
    DATEADD(DAY, -60, SYSDATETIME()), NULL, @adminId
FROM v
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.price_history ph
    WHERE ph.variant_id = v.id AND ph.reason = N'Initial price seed'
);

---------------------------------------------------------------
-- 12) LOYALTY_LEDGER (ensure >=10)
---------------------------------------------------------------
;WITH c AS (
    SELECT TOP (10) id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM dbo.customers
)
INSERT INTO dbo.loyalty_ledger (customer_id, points_delta, reason, reference_type, reference_id, note, created_by, created_at)
SELECT
    c.id,
    CASE WHEN c.rn % 2 = 0 THEN 100 ELSE 50 END,
    N'Purchase reward',
    N'ORDER',
    NULL,
    N'Seed loyalty points',
    @salesId,
    DATEADD(DAY, -1*c.rn, SYSDATETIME())
FROM c
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.loyalty_ledger ll
    WHERE ll.customer_id = c.id AND ll.note = N'Seed loyalty points'
);

---------------------------------------------------------------
-- 13) ORDERS (ensure >=10 seed orders)
---------------------------------------------------------------
;WITH cust AS (
    SELECT TOP (10) c.id AS customer_id, ROW_NUMBER() OVER (ORDER BY c.id) AS rn
    FROM dbo.customers c
)
INSERT INTO dbo.orders (
    customer_id, user_id, channel, status, payment_method, payment_status,
    shipping_address, shipping_fee, shipping_status, tracking_code,
    paid_at, shipped_at, delivered_at, cancelled_at, returned_at,
    subtotal, discount_total, tax_total, total_amount,
    applied_promotion_code, applied_promotion_json, notes
)
SELECT
    cust.customer_id,
    @salesId,
    CASE WHEN cust.rn % 2 = 0 THEN N'ONLINE' ELSE N'OFFLINE' END,
    CASE WHEN cust.rn % 5 = 0 THEN N'DELIVERED'
         WHEN cust.rn % 5 = 1 THEN N'PAID'
         ELSE N'PENDING' END,
    CASE WHEN cust.rn % 2 = 0 THEN N'BANK_TRANSFER' ELSE N'CASH' END,
    CASE WHEN cust.rn % 3 = 0 THEN N'PAID' ELSE N'UNPAID' END,
    (SELECT address FROM dbo.customers WHERE id = cust.customer_id),
    CASE WHEN cust.rn % 2 = 0 THEN 30000 ELSE 0 END,
    CASE WHEN cust.rn % 4 = 0 THEN N'SHIPPED' ELSE NULL END,
    CASE WHEN cust.rn % 4 = 0 THEN CONCAT(N'VN', RIGHT(N'000000' + CONVERT(NVARCHAR(10), cust.customer_id), 6)) ELSE NULL END,
    CASE WHEN cust.rn % 3 = 0 THEN DATEADD(DAY, -2, SYSDATETIME()) ELSE NULL END,
    CASE WHEN cust.rn % 4 = 0 THEN DATEADD(DAY, -1, SYSDATETIME()) ELSE NULL END,
    CASE WHEN cust.rn % 5 = 0 THEN SYSDATETIME() ELSE NULL END,
    NULL, NULL,
    0, 0, 0, 0,
    CASE WHEN cust.rn % 2 = 0 THEN N'WELCOME' ELSE NULL END,
    CASE WHEN cust.rn % 2 = 0 THEN N'{"code":"WELCOME","amount":100000}' ELSE NULL END,
    N'Seed order'
FROM cust
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.orders o
    WHERE o.customer_id = cust.customer_id AND o.notes = N'Seed order'
);

---------------------------------------------------------------
-- 14) ORDER_ITEMS (fix: no TOP+OFFSET; insert 1 item/order for 10 seed orders)
---------------------------------------------------------------
;WITH o AS (
    SELECT TOP (10) id AS order_id,
           ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
    FROM dbo.orders
    WHERE notes = N'Seed order'
),
v AS (
    SELECT TOP (10)
           pv.id AS variant_id,
           pv.product_id,
           pv.variant_name,
           pv.sku,
           pv.price,
           p.name AS product_name,
           ROW_NUMBER() OVER (ORDER BY pv.id) AS rn
    FROM dbo.product_variants pv
    JOIN dbo.products p ON p.id = pv.product_id
)
INSERT INTO dbo.order_items (
    order_id, variant_id, product_id, product_name, variant_name, sku,
    quantity, unit_price, discount, line_total
)
SELECT
    o.order_id,
    v.variant_id,
    v.product_id,
    v.product_name,
    v.variant_name,
    v.sku,
    CASE WHEN o.rn % 3 = 0 THEN 2 ELSE 1 END AS quantity,
    v.price AS unit_price,
    CASE WHEN o.rn % 2 = 0 THEN 50000 ELSE 0 END AS discount,
    (CASE WHEN o.rn % 3 = 0 THEN 2 ELSE 1 END) * (v.price - (CASE WHEN o.rn % 2 = 0 THEN 50000 ELSE 0 END)) AS line_total
FROM o
JOIN v ON v.rn = o.rn
WHERE NOT EXISTS (SELECT 1 FROM dbo.order_items oi WHERE oi.order_id = o.order_id);

-- Update totals for those orders
;WITH x AS (
    SELECT oi.order_id,
           SUM(oi.quantity * oi.unit_price) AS subtotal,
           SUM(oi.quantity * oi.discount)   AS discount_total
    FROM dbo.order_items oi
    WHERE oi.order_id IN (SELECT TOP (10) id FROM dbo.orders WHERE notes = N'Seed order' ORDER BY id DESC)
    GROUP BY oi.order_id
)
UPDATE o
SET
    o.subtotal       = x.subtotal,
    o.discount_total = x.discount_total,
    o.tax_total      = CAST(0 AS DECIMAL(15,2)),
    o.total_amount   = x.subtotal - x.discount_total + o.shipping_fee,
    o.updated_at     = SYSDATETIME()
FROM dbo.orders o
JOIN x ON x.order_id = o.id;

---------------------------------------------------------------
-- 15) PAYMENTS (ensure >=10 for seed orders)
---------------------------------------------------------------
INSERT INTO dbo.payments (order_id, amount, method, transaction_ref, status, paid_at)
SELECT TOP (10)
    o.id,
    o.total_amount,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY o.id) % 2 = 0 THEN N'BANK_TRANSFER' ELSE N'CASH' END,
    CONCAT(N'TXN-', CONVERT(NVARCHAR(20), o.id)),
    CASE WHEN o.payment_status = N'PAID' THEN N'SUCCESS' ELSE N'PENDING' END,
    CASE WHEN o.payment_status = N'PAID' THEN ISNULL(o.paid_at, DATEADD(DAY,-1,SYSDATETIME())) ELSE NULL END
FROM dbo.orders o
WHERE o.notes = N'Seed order'
  AND NOT EXISTS (SELECT 1 FROM dbo.payments p WHERE p.order_id = o.id)
ORDER BY o.id DESC;

---------------------------------------------------------------
-- 16) RETURNS (ensure >=10 based on order_items)
---------------------------------------------------------------
INSERT INTO dbo.returns (
    order_id, order_item_id, quantity, reason,
    refund_amount, refund_method, refund_status, refunded_at,
    status, note, processed_by
)
SELECT TOP (10)
    oi.order_id,
    oi.id,
    1,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY oi.id) % 2 = 0 THEN N'Lỗi kỹ thuật / đổi trả' ELSE N'Không vừa nhu cầu' END,
    oi.unit_price - oi.discount,
    N'BANK_TRANSFER',
    CASE WHEN ROW_NUMBER() OVER (ORDER BY oi.id) % 3 = 0 THEN N'REFUNDED' ELSE N'PENDING' END,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY oi.id) % 3 = 0 THEN DATEADD(DAY,-1,SYSDATETIME()) ELSE NULL END,
    N'PENDING',
    N'Seed return request',
    @salesId
FROM dbo.order_items oi
WHERE NOT EXISTS (SELECT 1 FROM dbo.returns r WHERE r.order_item_id = oi.id AND r.note = N'Seed return request')
ORDER BY oi.id DESC;

---------------------------------------------------------------
-- 17) CART_ITEMS (ensure >=10; avoid unique collision customer+variant)
---------------------------------------------------------------
;WITH c AS (
    SELECT TOP (10) id AS customer_id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM dbo.customers
),
v AS (
    SELECT TOP (10) id AS variant_id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM dbo.product_variants
)
INSERT INTO dbo.cart_items (customer_id, product_variant_id, quantity)
SELECT
    c.customer_id,
    v.variant_id,
    CASE WHEN c.rn % 3 = 0 THEN 2 ELSE 1 END
FROM c
JOIN v ON v.rn = c.rn
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.cart_items ci
    WHERE ci.customer_id = c.customer_id AND ci.product_variant_id = v.variant_id
);

PRINT N'Seed fix completed.';


-- ============================================================================
-- MIGRATION: Add missing columns to loyalty_ledger table
-- ============================================================================
USE retail_management_v5;
GO

-- Kiểm tra và thêm cột transaction_type
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.loyalty_ledger') AND name = 'transaction_type')
BEGIN
    ALTER TABLE dbo.loyalty_ledger
    ADD transaction_type NVARCHAR(30) NULL;
    
    PRINT 'Added column: transaction_type';
END
GO

-- Kiểm tra và thêm cột tier_before
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.loyalty_ledger') AND name = 'tier_before')
BEGIN
    ALTER TABLE dbo.loyalty_ledger
    ADD tier_before NVARCHAR(30) NULL;
    
    PRINT 'Added column: tier_before';
END
GO

-- Kiểm tra và thêm cột tier_after
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.loyalty_ledger') AND name = 'tier_after')
BEGIN
    ALTER TABLE dbo.loyalty_ledger
    ADD tier_after NVARCHAR(30) NULL;
    
    PRINT 'Added column: tier_after';
END
GO

-- ============================================================================
-- UPDATE EXISTING DATA: Set transaction_type based on points_delta
-- ============================================================================
UPDATE dbo.loyalty_ledger
SET transaction_type = CASE 
    WHEN points_delta > 0 THEN 'EARN'
    WHEN points_delta < 0 THEN 'DEDUCT'
    ELSE 'ADJUST'
END
WHERE transaction_type IS NULL;

PRINT 'Updated existing loyalty_ledger records with transaction_type';
GO

-- ============================================================================
-- UPDATE EXISTING DATA: Fix reference_type values (case sensitive)
-- ============================================================================
UPDATE dbo.loyalty_ledger
SET reference_type = 'orders'
WHERE reference_type = 'ORDER' OR reference_type = 'order';

PRINT 'Fixed reference_type to lowercase "orders"';
GO

-- ============================================================================
-- CREATE SAMPLE DATA WITH ORDER REFERENCES
-- ============================================================================
-- Xóa seed data cũ
DELETE FROM dbo.loyalty_ledger WHERE note = N'Seed loyalty points';

-- Tạo loyalty ledger mới với reference đến orders
DECLARE @salesId INT = (SELECT TOP 1 id FROM dbo.users WHERE username = N'sales01');

;WITH OrderCustomers AS (
    SELECT TOP (10)
        o.id AS order_id,
        o.customer_id,
        o.total_amount,
        o.created_at,
        ROW_NUMBER() OVER (ORDER BY o.id) AS rn
    FROM dbo.orders o
    WHERE o.customer_id IS NOT NULL
    ORDER BY o.id DESC
)
INSERT INTO dbo.loyalty_ledger (
    customer_id, 
    points_delta, 
    transaction_type,
    reason, 
    reference_type, 
    reference_id, 
    note, 
    created_by, 
    created_at
)
SELECT
    oc.customer_id,
    -- Tính điểm: 1% giá trị đơn hàng (làm tròn)
    CAST(ROUND(oc.total_amount * 0.01, 0) AS INT),
    'EARN',
    N'Purchase reward from order',
    'orders',  -- ✅ LOWERCASE và số nhiều
    oc.order_id,  -- ✅ REFERENCE_ID trỏ đến order.id
    CONCAT(N'Earned ', CAST(ROUND(oc.total_amount * 0.01, 0) AS INT), N' points from order #', oc.order_id),
    @salesId,
    oc.created_at
FROM OrderCustomers oc;

PRINT 'Created loyalty ledger entries linked to orders';
GO

-- ============================================================================
-- VERIFY DATA
-- ============================================================================
SELECT 
    ll.id,
    ll.customer_id,
    c.name AS customer_name,
    ll.points_delta,
    ll.transaction_type,
    ll.reason,
    ll.reference_type,
    ll.reference_id,
    ll.tier_before,
    ll.tier_after,
    ll.note,
    ll.created_at
FROM dbo.loyalty_ledger ll
JOIN dbo.customers c ON c.id = ll.customer_id
ORDER BY ll.created_at DESC;

PRINT 'Migration completed successfully!';