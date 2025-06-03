USE analytics;

-- 1. Orders Fact Table (Fixed data types and constraints)
CREATE TABLE IF NOT EXISTS orders_fact
(
    id               UInt64,
    user_id          UInt64,
    status           String,
    total_amount     Decimal(10, 2), -- Changed from Decimal(18,2) to match source
    shipping_address String,
    billing_address  String,
    payment_method   String,
    created_at       DateTime,
    updated_at       DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (created_at, id);

-- 2. Order Items Fact Table (Fixed data types)
CREATE TABLE IF NOT EXISTS order_items_fact
(
    id           UInt64,
    order_id     UInt64,
    product_id   UInt64,
    product_name String,
    quantity     UInt32,
    unit_price   Decimal(10, 2), -- Changed from Decimal(18,2) to match source
    created_at   DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (created_at, order_id, id);

-- 3. Inventory Fact Table (Fixed to match source schema)
CREATE TABLE IF NOT EXISTS inventory_fact
(
    id                 UInt64,           -- Added missing id field from source
    product_id         UInt64,
    sku                String,
    quantity_available Int32,            -- Renamed from available_quantity to match source
    reserved_quantity  Int32,
    reorder_threshold  Int32,
    warehouse_location Nullable(String), -- Added from source schema
    shelf_location     Nullable(String), -- Added from source schema
    status             Nullable(String), -- Added from source schema
    last_updated       DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(last_updated)
ORDER BY (product_id, id);

-- 4. Cart Items Fact Table (Added based on your source schema)
CREATE TABLE IF NOT EXISTS cart_items_fact
(
    id           UInt64,
    user_id      UInt64,
    product_id   UInt64,
    product_name String,
    price        Decimal(10, 2),
    quantity     UInt32,
    created_at   DateTime,
    updated_at   DateTime
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (created_at, user_id, id);

-- 5. Shipment Fact Table (Added based on your source schema)
CREATE TABLE IF NOT EXISTS shipment_fact
(
    id               UInt64,
    order_id         Nullable(UInt64),       -- Added order_id relationship
    shipping_address String,
    billing_address  String,
    status           String,
    created_at       DateTime DEFAULT now(), -- Non-nullable with default
    updated_at       Nullable(DateTime)
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(created_at)
ORDER BY (created_at, id);

