---
## 🛒 TECH STORE MANAGEMENT SYSTEM

## Hệ thống quản lý bán lẻ Laptop & Thiết bị công nghệ
---

## 📌 Tổng quan

**Tech Store Management System** là hệ thống quản trị toàn diện cho cửa hàng bán lẻ laptop & thiết bị công nghệ, hỗ trợ:

- Quản lý **sản phẩm đa biến thể (variant – SKU)**
- Quản lý **đơn hàng – trả hàng – tồn kho**
- Quản lý **khách hàng & Loyalty (VIP, điểm thưởng)**
- Quản lý **giá & khuyến mãi**
- Phân quyền người dùng & **Audit toàn hệ thống**
- Dashboard & báo cáo vận hành

Thiết kế tối ưu cho:

- Laptop / Ultrabook / Gaming / Workstation
- Chuẩn SKU theo **Model + CPU + RAM + SSD + Màu**
- Quản lý tồn kho chính xác theo từng SKU

---

## 🧱 Kiến trúc hệ thống (Logical Modules)

```
Tech Store System
├── Catalog / Product
├── Sales / Orders & Returns
├── Customer & Loyalty
├── Pricing & Promotion
├── User / Role / Audit
└── Dashboard & Reports
```

---

## 📦 1. Catalog / Product Management

### 1.1 Sản phẩm & Category

- Tạo / chỉnh sửa / xóa sản phẩm
- Tạo / chỉnh sửa / xóa category
- Gắn **1 sản phẩm – nhiều category**
- Ẩn / hiện sản phẩm theo trạng thái
- Upload hình đại diện cho sản phẩm & category
- Phân trang: **20 sản phẩm / trang**

### 1.2 Thuộc tính sản phẩm (Product Attributes)

| Nhóm           | Thuộc tính                             |
| -------------- | -------------------------------------- |
| Cơ bản         | Tên, SKU chuẩn, Thương hiệu            |
| Loại           | Laptop, Ultrabook, Gaming, Workstation |
| Series / Model | MacBook Air, Dell XPS, ThinkPad        |
| CPU            | Intel i5/i7, AMD Ryzen, Apple M1/M2    |
| RAM            | 8GB / 16GB / 32GB                      |
| SSD            | 256GB / 512GB / 1TB                    |
| GPU            | Integrated / RTX / Radeon              |
| Màn hình       | 13”, 14”, 15.6”                        |
| Màu sắc        | Silver, Space Gray, Black              |

---

### 1.3 Variant & SKU (CỰC KỲ QUAN TRỌNG)

> **Mỗi biến thể = 1 SKU riêng**

```
SKU = Model + CPU + RAM + SSD + Color
```

#### Ví dụ:

| Variant                          | SKU                       | Giá        | Tồn |
| -------------------------------- | ------------------------- | ---------- | --- |
| MacBook Air M2 16GB 512GB Silver | MB-AIR-M2-16GB-512GB-SIL  | 35,000,000 | 5   |
| MacBook Air M2 16GB 512GB Gray   | MB-AIR-M2-16GB-512GB-GRAY | 35,000,000 | 3   |
| MacBook Air M2 8GB 256GB Silver  | MB-AIR-M2-8GB-256GB-SIL   | 30,000,000 | 2   |

**Nguyên tắc chuẩn hóa:**

- SKU **DUY NHẤT**
- Mỗi variant quản lý **giá & tồn riêng**
- Tránh nhầm lẫn tồn kho
- Hỗ trợ **Pre-order** & số lượng giới hạn

---

### 1.4 Hình ảnh sản phẩm

- 1 hình chính + 3–5 hình chi tiết
- Chuẩn background & tỉ lệ ảnh
- Gallery hiển thị frontend
- Chỉnh sửa / xóa từng ảnh
- Batch upload ảnh

---

### 1.5 Tìm kiếm & lọc

- Tìm theo: **Tên, SKU**
- Tìm nâng cao theo:

  - CPU / RAM / SSD
  - Category
  - Tag

- Lọc:

  - Còn hàng / Hết hàng
  - Sản phẩm mới
  - Bán chạy

- Sắp xếp:

  - Giá ↑ ↓
  - Mới nhất
  - Bán chạy

---

## 🧾 2. Sales / Orders & Returns

### 2.1 Đơn hàng

- Tạo đơn hàng (nhiều sản phẩm)
- Chọn khách hàng
- Phương thức thanh toán
- Tính tổng tiền tự động
- In hóa đơn PDF
- Email xác nhận đơn hàng

### 2.2 Trạng thái đơn hàng

```
Mới → Đang xử lý → Đang giao → Hoàn tất
           ↓
        Hủy / Trả hàng
```

- Tự động cập nhật tồn kho
- Cập nhật doanh thu theo trạng thái

---

### 2.3 Trả hàng & hoàn tiền

- Trả từng sản phẩm trong đơn
- Ghi chú trả hàng
- Cập nhật:

  - Tồn kho
  - Doanh thu
  - Điểm Loyalty

---

### 2.4 Báo cáo & thống kê

- Doanh thu theo:

  - Ngày / Tuần / Tháng
  - Nhân viên bán
  - Kênh bán (Online / Offline)

- Top sản phẩm bán chạy
- Báo cáo hoàn trả

---

## 👥 3. Customer & Loyalty

### 3.1 Quản lý khách hàng

- Thông tin: tên, email, phone, ngày sinh
- Ghi chú nội bộ
- Lịch sử giao dịch chi tiết

### 3.2 Loyalty & VIP

- Tích điểm tự động
- Phân loại:

  - Thường
  - VIP

- Ưu đãi theo mốc điểm
- Tự động:

  - Email sinh nhật
  - Nhắc khách chưa mua 30/60/90 ngày
  - Tặng ưu đãi

---

### 3.3 Báo cáo khách hàng

- Top khách hàng theo doanh thu
- Top khách trung thành
- Thống kê điểm thưởng
- Dashboard khách hàng

---

## 💰 4. Pricing & Promotion

### 4.1 Giá sản phẩm

- Quản lý giá theo:

  - Sản phẩm
  - Variant
  - Nhóm khách hàng

- Lịch sử giá

### 4.2 Khuyến mãi

- Giảm %
- Combo (mua 2 tặng 1)
- Áp dụng theo:

  - Sản phẩm
  - Nhóm khách
  - Chiến dịch

- Tự động kiểm tra xung đột
- Cảnh báo khuyến mãi sắp hết hạn

---

## 🔐 5. User / Role / Audit

### 5.1 Phân quyền

| Role      | Quyền             |
| --------- | ----------------- |
| Admin     | Toàn quyền        |
| Sales     | Đơn hàng, khách   |
| Inventory | Sản phẩm, tồn kho |

---

### 5.2 Audit & Log

- Ghi log:

  - Tạo / sửa / xóa dữ liệu
  - Thay đổi quyền

- Lọc log theo:

  - User
  - Module
  - Thời gian

- Export CSV
- Cảnh báo thao tác nhạy cảm
- Dashboard audit

---

## 📊 6. Dashboard & Reports

- Dashboard sản phẩm
- Dashboard đơn hàng
- Dashboard khách hàng
- Dashboard log & bảo mật
- Báo cáo tổng hợp tuần / tháng

---

## 🎯 Mục tiêu thiết kế

- ✔ Chuẩn hóa SKU & Variant
- ✔ Quản lý tồn kho chính xác
- ✔ Dễ mở rộng thêm ngành hàng
- ✔ Phù hợp cho hệ thống lớn
- ✔ Sẵn sàng tích hợp ERP / CRM

---

## 🚀 Định hướng mở rộng

- API Mobile App
- AI gợi ý sản phẩm
- Phân tích hành vi khách hàng
- Đồng bộ đa chi nhánh

---

## 📄 License

Internal Project – Tech Store
© 2026 Tech Store Management System

---
