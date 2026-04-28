---------------------------------------------------------------
-- 0) CREATE DB (optional)
---------------------------------------------------------------
IF DB_ID(N'retail_management_v2') IS NULL
BEGIN
    CREATE DATABASE retail_management_v2;
END
GO

USE retail_management_v2;
GO

---------------------------------------------------------------
-- 0.1) CLEANUP (drop theo thứ tự phụ thuộc) - tùy chọn
---------------------------------------------------------------
IF OBJECT_ID('dbo.returns', 'U') IS NOT NULL DROP TABLE dbo.returns;
IF OBJECT_ID('dbo.payments', 'U') IS NOT NULL DROP TABLE dbo.payments;
IF OBJECT_ID('dbo.order_items', 'U') IS NOT NULL DROP TABLE dbo.order_items;
IF OBJECT_ID('dbo.orders', 'U') IS NOT NULL DROP TABLE dbo.orders;

IF OBJECT_ID('dbo.stock_transactions', 'U') IS NOT NULL DROP TABLE dbo.stock_transactions;
IF OBJECT_ID('dbo.images', 'U') IS NOT NULL DROP TABLE dbo.images;

IF OBJECT_ID('dbo.price_history', 'U') IS NOT NULL DROP TABLE dbo.price_history;
IF OBJECT_ID('dbo.promotions', 'U') IS NOT NULL DROP TABLE dbo.promotions;

IF OBJECT_ID('dbo.product_tags', 'U') IS NOT NULL DROP TABLE dbo.product_tags;
IF OBJECT_ID('dbo.product_categories', 'U') IS NOT NULL DROP TABLE dbo.product_categories;

IF OBJECT_ID('dbo.product_variants', 'U') IS NOT NULL DROP TABLE dbo.product_variants;
IF OBJECT_ID('dbo.products', 'U') IS NOT NULL DROP TABLE dbo.products;

IF OBJECT_ID('dbo.tags', 'U') IS NOT NULL DROP TABLE dbo.tags;
IF OBJECT_ID('dbo.categories', 'U') IS NOT NULL DROP TABLE dbo.categories;

IF OBJECT_ID('dbo.loyalty_ledger', 'U') IS NOT NULL DROP TABLE dbo.loyalty_ledger;
IF OBJECT_ID('dbo.customers', 'U') IS NOT NULL DROP TABLE dbo.customers;

IF OBJECT_ID('dbo.audit_logs', 'U') IS NOT NULL DROP TABLE dbo.audit_logs;
IF OBJECT_ID('dbo.password_resets', 'U') IS NOT NULL DROP TABLE dbo.password_resets;
IF OBJECT_ID('dbo.user_logins', 'U') IS NOT NULL DROP TABLE dbo.user_logins;
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL DROP TABLE dbo.users;

IF OBJECT_ID('dbo.order_seq', 'SO') IS NOT NULL DROP SEQUENCE dbo.order_seq;
GO

---------------------------------------------------------------
-- 0.2) SEQUENCES
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
    id              INT IDENTITY(1,1) PRIMARY KEY,
    username        NVARCHAR(50)  NOT NULL UNIQUE,
    email           NVARCHAR(100) NOT NULL UNIQUE,
    password_hash   NVARCHAR(255) NOT NULL,

    -- đơn giản hoá role: ADMIN / SALES / INVENTORY / CUSTOMER
    role            NVARCHAR(30)  NOT NULL DEFAULT 'CUSTOMER',

    is_active       BIT NOT NULL DEFAULT 1,
    created_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- Lịch sử login (đủ để báo cáo, cảnh báo bất thường)
CREATE TABLE dbo.user_logins (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id     INT NULL,
    username    NVARCHAR(50) NULL,         -- nếu login fail chưa map được user
    success     BIT NOT NULL,
    ip_address  NVARCHAR(45) NULL,
    user_agent  NVARCHAR(400) NULL,
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_user_logins_user FOREIGN KEY (user_id)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_user_logins_user_date ON dbo.user_logins(user_id, created_at DESC);
CREATE INDEX IX_user_logins_success_date ON dbo.user_logins(success, created_at DESC);
GO

-- Reset password token (hash token để an toàn)
CREATE TABLE dbo.password_resets (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      INT NOT NULL,
    token_hash   NVARCHAR(255) NOT NULL,
    expires_at   DATETIME2 NOT NULL,
    used_at      DATETIME2 NULL,
    requested_ip NVARCHAR(45) NULL,
    created_at   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_password_resets_user FOREIGN KEY (user_id)
        REFERENCES dbo.users(id) ON DELETE CASCADE
);
GO
CREATE INDEX IX_password_resets_user_date ON dbo.password_resets(user_id, created_at DESC);
GO

-- Audit log chung: dùng cho lịch sử sửa đổi product/order/customer/role...
CREATE TABLE dbo.audit_logs (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id      INT NULL,
    module       NVARCHAR(50) NOT NULL,     -- PRODUCT / ORDER / CUSTOMER / AUTH / SYSTEM...
    action       NVARCHAR(50) NOT NULL,     -- CREATE / UPDATE / DELETE / ROLE_CHANGE...
    target_type  NVARCHAR(50) NULL,         -- products / orders / customers...
    target_id    BIGINT NULL,

    -- lưu diff / payload dạng JSON cho dễ mở rộng
    details_json NVARCHAR(MAX) NULL,
    ip_address   NVARCHAR(45) NULL,

    created_at   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
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
    id              INT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(150) NOT NULL,
    email           NVARCHAR(100) NULL UNIQUE,
    phone           NVARCHAR(30)  NULL,

    date_of_birth   DATE NULL,

    -- phân loại đơn giản
    customer_type   NVARCHAR(20) NOT NULL DEFAULT 'REGULAR', -- REGULAR/VIP
    vip_tier        NVARCHAR(30) NULL,                       -- GOLD/SILVER/... (tuỳ bạn)
    segments_json   NVARCHAR(MAX) NULL,                      -- nếu muốn gán nhiều chương trình đặc biệt

    loyalty_points  INT NOT NULL DEFAULT 0,
    total_spent     DECIMAL(15,2) NOT NULL DEFAULT 0,
    last_order_at   DATETIME2 NULL,

    address         NVARCHAR(500) NULL,
    notes           NVARCHAR(MAX) NULL,

    is_active       BIT NOT NULL DEFAULT 1,
    created_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX IX_customers_type ON dbo.customers(customer_type);
CREATE INDEX IX_customers_points ON dbo.customers(loyalty_points DESC);
CREATE INDEX IX_customers_spent ON dbo.customers(total_spent DESC);
CREATE INDEX IX_customers_last_order ON dbo.customers(last_order_at DESC);
GO

-- Sổ cái điểm (đủ để: cộng/trừ điểm, hoàn điểm khi trả hàng, lịch sử chi tiết)
CREATE TABLE dbo.loyalty_ledger (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id    INT NOT NULL,
    points_delta   INT NOT NULL,             -- + / -
    reason         NVARCHAR(200) NULL,       -- PURCHASE / RETURN / PROMO / ADJUST...
    reference_type NVARCHAR(50) NULL,        -- orders / returns...
    reference_id   BIGINT NULL,
    note           NVARCHAR(500) NULL,
    created_by     INT NULL,
    created_at     DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_loyalty_customer FOREIGN KEY (customer_id)
        REFERENCES dbo.customers(id) ON DELETE CASCADE,
    CONSTRAINT FK_loyalty_user FOREIGN KEY (created_by)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_loyalty_customer_date ON dbo.loyalty_ledger(customer_id, created_at DESC);
GO

---------------------------------------------------------------
-- 3) CATEGORIES & TAGS
---------------------------------------------------------------
CREATE TABLE dbo.categories (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    name          NVARCHAR(150) NOT NULL,
    description   NVARCHAR(500) NULL,
    image_url     NVARCHAR(500) NULL,

    parent_id     INT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_active     BIT NOT NULL DEFAULT 1,

    created_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_categories_parent FOREIGN KEY (parent_id)
        REFERENCES dbo.categories(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_categories_parent ON dbo.categories(parent_id);
GO

CREATE TABLE dbo.tags (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    name       NVARCHAR(80) NOT NULL UNIQUE,
    tag_type   NVARCHAR(30) NOT NULL DEFAULT 'GENERAL', -- CAMPAIGN/PROMO/GENERAL...
    is_active  BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

---------------------------------------------------------------
-- 4) PRODUCTS & VARIANTS
---------------------------------------------------------------
CREATE TABLE dbo.products (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    name            NVARCHAR(200) NOT NULL,
    sku             NVARCHAR(100) NOT NULL UNIQUE,

    description     NVARCHAR(MAX) NULL,

    is_visible      BIT NOT NULL DEFAULT 1,
    is_featured     BIT NOT NULL DEFAULT 0,

    view_count      INT NOT NULL DEFAULT 0,
    sold_count      INT NOT NULL DEFAULT 0,

    -- thuộc tính chung (phục vụ tìm kiếm nâng cao nếu bạn dùng OPENJSON/JSON_VALUE)
    attributes_json NVARCHAR(MAX) NULL,

    created_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    deleted_at      DATETIME2 NULL
);
GO
CREATE INDEX IX_products_visible ON dbo.products(is_visible, created_at DESC);
CREATE INDEX IX_products_created ON dbo.products(created_at DESC);
CREATE INDEX IX_products_sold    ON dbo.products(sold_count DESC);
CREATE INDEX IX_products_name    ON dbo.products(name);
GO

-- N-N product <-> category (lọc nhiều category, 1 product nhiều category)
CREATE TABLE dbo.product_categories (
    product_id  INT NOT NULL,
    category_id INT NOT NULL,
    is_primary  BIT NOT NULL DEFAULT 0,
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT FK_pc_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_pc_category FOREIGN KEY (category_id)
        REFERENCES dbo.categories(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_pc_category ON dbo.product_categories(category_id, product_id);
GO

-- N-N product <-> tag (tag theo chiến dịch, tag khuyến mãi, lọc theo tag)
CREATE TABLE dbo.product_tags (
    product_id INT NOT NULL,
    tag_id     INT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (product_id, tag_id),
    CONSTRAINT FK_pt_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_pt_tag FOREIGN KEY (tag_id)
        REFERENCES dbo.tags(id) ON DELETE NO ACTION
);
GO
CREATE INDEX IX_pt_tag ON dbo.product_tags(tag_id, product_id);
GO

CREATE TABLE dbo.product_variants (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    product_id      INT NOT NULL,

    variant_name    NVARCHAR(150) NOT NULL,
    sku             NVARCHAR(100) NOT NULL UNIQUE,
    barcode         NVARCHAR(100) NULL,

    -- giá hiện tại
    currency_code   CHAR(3) NOT NULL DEFAULT 'VND',
    price           DECIMAL(12,2) NOT NULL,
    cost_price      DECIMAL(12,2) NULL,

    -- giá theo nhóm khách hàng (ít bảng): JSON {"VIP":9000000,"REGULAR":9500000}
    price_tiers_json NVARCHAR(MAX) NULL,

    -- tồn kho đơn giản
    stock_quantity  INT NOT NULL DEFAULT 0,
    reserved_qty    INT NOT NULL DEFAULT 0,

    -- thuộc tính variant: RAM/SSD/Color...
    attributes_json NVARCHAR(MAX) NULL,

    is_active       BIT NOT NULL DEFAULT 1,
    created_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    deleted_at      DATETIME2 NULL,

    CONSTRAINT FK_variants_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE CASCADE
);
GO
CREATE INDEX IX_variants_product ON dbo.product_variants(product_id);
CREATE INDEX IX_variants_stock   ON dbo.product_variants(stock_quantity, reserved_qty);
CREATE INDEX IX_variants_created ON dbo.product_variants(created_at DESC);
GO

---------------------------------------------------------------
-- 4.1) IMAGES (gallery + ảnh đại diện cho product/variant)
-- 1 bảng duy nhất để giảm phức tạp
---------------------------------------------------------------
CREATE TABLE dbo.images (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id  INT NULL,
    variant_id  INT NULL,

    url         NVARCHAR(800) NOT NULL,
    is_primary  BIT NOT NULL DEFAULT 0,
    sort_order  INT NOT NULL DEFAULT 0,
    alt_text    NVARCHAR(200) NULL,
    meta_json   NVARCHAR(MAX) NULL,

    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_images_product FOREIGN KEY (product_id)
        REFERENCES dbo.products(id) ON DELETE NO ACTION,
    CONSTRAINT FK_images_variant FOREIGN KEY (variant_id)
        REFERENCES dbo.product_variants(id) ON DELETE NO ACTION,

    CONSTRAINT CK_images_owner CHECK (
        (product_id IS NOT NULL AND variant_id IS NULL)
        OR
        (product_id IS NULL AND variant_id IS NOT NULL)
    )
);
GO
CREATE INDEX IX_images_product ON dbo.images(product_id, is_primary DESC, sort_order, id DESC);
CREATE INDEX IX_images_variant ON dbo.images(variant_id, is_primary DESC, sort_order, id DESC);
GO

---------------------------------------------------------------
-- 5) INVENTORY TRANSACTIONS (IMPORT/EXPORT/ADJUST/RESERVE/RELEASE/RETURN)
---------------------------------------------------------------
CREATE TABLE dbo.stock_transactions (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    variant_id     INT NOT NULL,

    -- quantity (+/-). Ví dụ: IMPORT +10, EXPORT -2, RETURN +1
    quantity       INT NOT NULL,
    type           NVARCHAR(30) NOT NULL,   -- IMPORT/EXPORT/ADJUST/RESERVE/RELEASE/RETURN/CANCEL

    reference_type NVARCHAR(50) NULL,       -- orders/returns/manual...
    reference_id   BIGINT NULL,

    note           NVARCHAR(500) NULL,
    created_by     INT NULL,
    created_at     DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_st_variant FOREIGN KEY (variant_id)
        REFERENCES dbo.product_variants(id) ON DELETE CASCADE,
    CONSTRAINT FK_st_user FOREIGN KEY (created_by)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_st_variant_date ON dbo.stock_transactions(variant_id, created_at DESC);
CREATE INDEX IX_st_ref ON dbo.stock_transactions(reference_type, reference_id);
GO

---------------------------------------------------------------
-- 6) PROMOTIONS + PRICE HISTORY
---------------------------------------------------------------
CREATE TABLE dbo.promotions (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    code            NVARCHAR(50) NOT NULL UNIQUE,
    name            NVARCHAR(150) NOT NULL,
    description     NVARCHAR(500) NULL,

    discount_type   NVARCHAR(20) NOT NULL,  -- PERCENT/AMOUNT
    discount_value  DECIMAL(12,2) NOT NULL,

    min_order_amount DECIMAL(15,2) NULL,

    -- áp dụng sản phẩm/variant bằng JSON để giảm bảng
    -- ví dụ: {"scope":"ALL"} hoặc {"product_ids":[1,2]} hoặc {"variant_ids":[10,11]}
    applicability_json NVARCHAR(MAX) NULL,

    -- rule nâng cao (combo, ưu tiên, stackable...) cũng lưu JSON
    rules_json      NVARCHAR(MAX) NULL,
    priority        INT NOT NULL DEFAULT 0,
    stackable       BIT NOT NULL DEFAULT 0,

    start_date      DATETIME2 NOT NULL,
    end_date        DATETIME2 NOT NULL,

    usage_limit     INT NULL,
    used_count      INT NOT NULL DEFAULT 0,

    is_active       BIT NOT NULL DEFAULT 1,
    created_by      INT NULL,
    created_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_promotions_user FOREIGN KEY (created_by)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_promotions_active_dates ON dbo.promotions(is_active, start_date, end_date);
GO

-- Lịch sử giá (đáp ứng: lịch sử giá, cảnh báo giá thấp hơn giá nhập, rollback...)
CREATE TABLE dbo.price_history (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    variant_id   INT NOT NULL,

    currency_code CHAR(3) NOT NULL DEFAULT 'VND',
    price        DECIMAL(12,2) NOT NULL,
    cost_price   DECIMAL(12,2) NULL,

    reason       NVARCHAR(200) NULL,        -- MANUAL / PROMO / AUTO / IMPORT_COST_UPDATE...
    effective_from DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    effective_to   DATETIME2 NULL,

    created_by   INT NULL,
    created_at   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_ph_variant FOREIGN KEY (variant_id)
        REFERENCES dbo.product_variants(id) ON DELETE CASCADE,
    CONSTRAINT FK_ph_user FOREIGN KEY (created_by)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_ph_variant_date ON dbo.price_history(variant_id, effective_from DESC);
GO

---------------------------------------------------------------
-- 7) ORDERS / ITEMS / PAYMENTS / RETURNS
---------------------------------------------------------------
CREATE TABLE dbo.orders (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,

    order_number   NVARCHAR(40) NOT NULL UNIQUE
        DEFAULT (
            CONCAT(
                N'ORD-',
                CONVERT(CHAR(8), SYSDATETIME(), 112),
                N'-',
                RIGHT(N'000000' + CONVERT(VARCHAR(20), NEXT VALUE FOR dbo.order_seq), 6)
            )
        ),

    customer_id    INT NULL,
    user_id        INT NULL, -- nhân viên bán

    channel        NVARCHAR(20) NOT NULL DEFAULT 'OFFLINE', -- ONLINE/OFFLINE/OTHER

    status         NVARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING/PAID/SHIPPING/DELIVERED/CANCELLED/RETURNED
    payment_method NVARCHAR(50) NULL,
    payment_status NVARCHAR(20) NOT NULL DEFAULT 'UNPAID',  -- UNPAID/PARTIAL/PAID/REFUNDED

    -- shipping info
    shipping_address NVARCHAR(500) NULL,
    shipping_fee   DECIMAL(12,2) NOT NULL DEFAULT 0,
    shipping_status NVARCHAR(30) NULL,       -- PACKING/SHIPPED/DELIVERED...
    tracking_code  NVARCHAR(100) NULL,

    -- mốc thời gian trạng thái
    paid_at        DATETIME2 NULL,
    shipped_at     DATETIME2 NULL,
    delivered_at   DATETIME2 NULL,
    cancelled_at   DATETIME2 NULL,
    returned_at    DATETIME2 NULL,

    -- totals
    subtotal       DECIMAL(15,2) NOT NULL DEFAULT 0,
    discount_total DECIMAL(15,2) NOT NULL DEFAULT 0,
    tax_total      DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount   DECIMAL(15,2) NOT NULL DEFAULT 0,

    applied_promotion_code NVARCHAR(50) NULL,
    applied_promotion_json NVARCHAR(MAX) NULL, -- snapshot promo để in hóa đơn

    notes          NVARCHAR(MAX) NULL,

    created_at     DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at     DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_orders_customer FOREIGN KEY (customer_id)
        REFERENCES dbo.customers(id) ON DELETE SET NULL,
    CONSTRAINT FK_orders_user FOREIGN KEY (user_id)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_orders_status_date ON dbo.orders(status, created_at DESC);
CREATE INDEX IX_orders_customer_date ON dbo.orders(customer_id, created_at DESC);
CREATE INDEX IX_orders_user_date ON dbo.orders(user_id, created_at DESC);
CREATE INDEX IX_orders_channel_date ON dbo.orders(channel, created_at DESC);
GO

CREATE TABLE dbo.order_items (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id      BIGINT NOT NULL,

    variant_id    INT NULL,
    product_id    INT NULL,

    product_name  NVARCHAR(200) NOT NULL,
    variant_name  NVARCHAR(150) NULL,
    sku           NVARCHAR(100) NULL,

    quantity      INT NOT NULL,
    unit_price    DECIMAL(12,2) NOT NULL,
    discount      DECIMAL(12,2) NOT NULL DEFAULT 0,
    line_total    DECIMAL(15,2) NOT NULL,

    created_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

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
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,
    method          NVARCHAR(50) NOT NULL,
    transaction_ref NVARCHAR(100) NULL,
    status          NVARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING/SUCCESS/FAILED/REFUNDED
    paid_at         DATETIME2 NULL,
    created_at      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_payments_order FOREIGN KEY (order_id)
        REFERENCES dbo.orders(id) ON DELETE CASCADE
);
GO
CREATE INDEX IX_payments_order_date ON dbo.payments(order_id, created_at DESC);
GO

CREATE TABLE dbo.returns (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    order_id       BIGINT NOT NULL,
    order_item_id  BIGINT NOT NULL,

    quantity       INT NOT NULL,
    reason         NVARCHAR(500) NULL,

    refund_amount  DECIMAL(15,2) NOT NULL DEFAULT 0,
    refund_method  NVARCHAR(50) NULL,
    refund_status  NVARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED/REFUNDED
    refunded_at    DATETIME2 NULL,

    status         NVARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED/DONE
    note           NVARCHAR(MAX) NULL,

    processed_by   INT NULL,
    created_at     DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

    CONSTRAINT FK_returns_order FOREIGN KEY (order_id)
        REFERENCES dbo.orders(id) ON DELETE NO ACTION,
    CONSTRAINT FK_returns_item FOREIGN KEY (order_item_id)
        REFERENCES dbo.order_items(id) ON DELETE NO ACTION,
    CONSTRAINT FK_returns_user FOREIGN KEY (processed_by)
        REFERENCES dbo.users(id) ON DELETE SET NULL
);
GO
CREATE INDEX IX_returns_order ON dbo.returns(order_id, created_at DESC);
CREATE INDEX IX_returns_item ON dbo.returns(order_item_id);
GO

IF OBJECT_ID('dbo.system_settings', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.system_settings (
        setting_key   NVARCHAR(50) NOT NULL PRIMARY KEY,
        setting_value NVARCHAR(100) NOT NULL,
        updated_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.system_settings WHERE setting_key = 'DEFAULT_CURRENCY')
BEGIN
    INSERT INTO dbo.system_settings(setting_key, setting_value) VALUES ('DEFAULT_CURRENCY', 'VND');
END
GO

ALTER TABLE dbo.user_logins
ADD updated_at DATETIME2 NOT NULL
    CONSTRAINT DF_user_logins_updated_at DEFAULT SYSDATETIME();
GO