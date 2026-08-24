# Hệ Thống Điều Phối & Giám Sát Quy Trình Sản Xuất Xưởng Gốm

Hệ thống quản lý và tự động hóa quy trình gia công, sản xuất gốm sứ theo từng công đoạn liên hoàn tại xưởng, tích hợp phân tích thông số bằng AI, theo dõi tiến độ sản xuất dạng Kanban và tự động gửi cảnh báo qua Telegram/Slack/Zalo.

---

## 1. Kiến Trúc Tổng Thể

```
[ React FE ] --REST API/WebSocket--> [ Spring Boot Backend API ]
                                                |
            +-----------------------------------+-----------------------------------+
            |                                   |                                   |
    [ MySQL Database ]             [ OpenAI / Claude LLM API ]              [ Slack/Zalo Bot API ]
 (orders, batches, extractions,      (Phân tích thông số đơn hàng)          (Thông báo tiến độ & Cảnh báo đỏ)
   stage_history, qc, alerts)
```

---

## 2. Luồng Nghiệp Vụ Hệ Thống

1. **Tiếp Nhận & Phân Tích Đơn Hàng**:
   - Người dùng nhập mô tả đơn hàng bằng văn bản tự nhiên qua API `POST /api/orders` (ví dụ: *"Đơn 200 Bình gốm họa tiết sen men lam cao 35cm, nung 1280°C trong 10 ngày"*).
   - Backend gọi dịch vụ `AiExtractionService` gửi yêu cầu tới LLM để bóc tách các thông số kỹ thuật (tên sản phẩm, số lượng, chiều cao, loại men, nhiệt độ nung, thời gian nung, lượng đất sét dự kiến, mức ưu tiên...).
   - **Xử lý tin cậy**: Kiểm tra cú pháp JSON, xác thực các trường dữ liệu bắt buộc và tự động thử lại 3 lần nếu phản hồi từ AI không đúng định dạng. Hệ thống tích hợp sẵn cơ chế phân tích dự phòng (Smart Fallback Engine) để hoạt động ngay cả khi không có kết nối API ngoài.

2. **Khởi Tạo Quy Trình 6 Công Đoạn Sản Xuất**:
   - Tự động tạo mẻ sản xuất mới (`Batch`) theo mã quản lý dạng `#GOM-YYYYMMDD-XX`.
   - Khởi tạo 6 công đoạn nối tiếp nhau:
     1. `Tạo hình mộc` (FORMING - Trạng thái: `IN_PROGRESS`)
     2. `Phơi sấy & Sửa mộc` (DRYING_TRIMMING - Trạng thái: `PENDING`)
     3. `Vẽ họa tiết` (PAINTING - Trạng thái: `PENDING`)
     4. `Tráng men` (GLAZING - Trạng thái: `PENDING`)
     5. `Vào lò nung` (FIRING - Trạng thái: `PENDING`)
     6. `Kiểm định chất lượng (QC) & Đóng gói` (QC_PACKAGING - Trạng thái: `PENDING`)

3. **Điều Phối Công Đoạn Xưởng (Pipeline Service)**:
   - Thợ hoặc quản lý cập nhật tiến độ công đoạn qua API `PATCH /api/batches/{id}/advance` hoặc qua giao diện Web / Telegram.
   - Sử dụng khóa bi quan (Pessimistic Locking `@Lock(LockModeType.PESSIMISTIC_WRITE)`) để đảm bảo không bị xung đột dữ liệu khi nhiều người thao tác đồng thời.
   - Ràng buộc quy trình: Không cho phép bỏ qua công đoạn tùy tiện (trừ khi xác nhận lý do), từ chối chuyển trạng thái nếu mẻ gốm đang tạm dừng (`ON_HOLD`) hoặc gặp sự cố (`FAILED`).

4. **Kiểm Định QC & Cảnh Báo Sự Cố Khẩn Cấp**:
   - Cập nhật số lượng kiểm định đạt/lỗi qua `POST /api/qc`.
   - Tự động tính tỷ lệ sản phẩm lỗi (ví dụ: nứt men, hỏng mộc). Khi tỷ lệ lỗi vượt ngưỡng **3%**, hệ thống đánh dấu bản ghi khẩn cấp (`is_critical = true`) và tự động gửi tin nhắn cảnh báo đỏ đến nhóm chat Telegram của quản lý xưởng.

5. **Tích Hợp Telegram Bot & Webhook Nút Bấm Inline**:
   - Khi một công đoạn hoàn thành, hệ thống gửi thông báo kèm nút bấm xác nhận trực tiếp trong ứng dụng Telegram.
   - Tiếp nhận callback qua webhook `POST /api/telegram/webhook` để thợ hoặc quản lý duyệt chuyển công đoạn ngay trong chat.

6. **Giao Diện Dashboard & Bảng Kanban**:
   - API `GET /api/dashboard/stats`: Thống kê tổng số đơn hàng, mẻ sản xuất đang thực hiện, mẻ hoàn thành, tỷ lệ đạt QC và số lượng cảnh báo khẩn cấp.
   - API `GET /api/dashboard/kanban`: Cung cấp dữ liệu dạng bảng Kanban hiển thị vị trí của từng mẻ gốm theo cột công đoạn sản xuất.

---

## 3. Cấu Trúc Mã Nguồn Backend (Spring Boot)

```
com.ceramic
├── config/              # Cấu hình ứng dụng (ModelMapper, Jackson, Async ThreadPool)
├── controller/          # REST API Controllers (Order, Batch, QC, Dashboard, Telegram)
├── dto/                 # Đối tượng chuyển đổi dữ liệu (Request/Response DTOs)
│   ├── ApiResponse.java                  # Cấu trúc phản hồi API chuẩn
│   ├── OrderCreateRequest.java / OrderResponse.java
│   ├── AiExtractionResultDto.java        # DTO chứa kết quả phân tích AI
│   ├── BatchAdvanceRequest.java / BatchResponse.java
│   ├── QcRecordRequest.java / QcRecordResponse.java
│   └── DashboardStatsResponse.java / KanbanBoardResponse.java
├── entity/              # Mô hình dữ liệu JPA (User, Order, AiExtraction, Batch, Stage, BatchStageHistory, QcRecord, Alert)
├── enums/               # Danh mục trạng thái và quyền hạn
├── exception/           # Xử lý ngoại lệ tập trung (GlobalExceptionHandler)
├── integration/         # Tích hợp dịch vụ ngoài (LlmClient, TelegramClient)
├── repository/          # Tầng truy xuất dữ liệu Spring Data JPA
└── service/             # Tầng nghiệp vụ (Tách biệt Interface và Implementation)
    ├── AiExtractionService.java + Impl  # Bóc tách thông số AI & kiểm tra dữ liệu
    ├── PipelineService.java + Impl      # Quản lý trạng thái và chuyển công đoạn
    ├── QcService.java + Impl            # Kiểm định chất lượng & đánh giá ngưỡng lỗi
    ├── NotificationService.java + Impl  # Gửi thông báo Telegram bất đồng bộ
    └── DashboardService.java + Impl     # Tổng hợp dữ liệu thống kê & Kanban
```

---

## 4. Khởi Tạo Cơ Sở Dữ Liệu MySQL

File SQL đầy đủ được lưu tại **[`ceramics_db.sql`](file:///c:/langgom/ceramics_db.sql)** cũng như hai file cấu trúc và dữ liệu mẫu trong thư mục tài nguyên dự án `src/main/resources/schema.sql` & `data.sql`.

### Tạo cơ sở dữ liệu và import thủ công
```sql
CREATE DATABASE IF NOT EXISTS ceramics_pipeline DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ceramics_pipeline;
-- Chạy nội dung file ceramics_db.sql
```

### Chạy tự động cùng Spring Boot
Cấu hình thông tin kết nối MySQL trong file `application-dev.yml`. Khi ứng dụng khởi chạy, hệ thống sẽ tự động khởi tạo bảng và chèn dữ liệu 6 công đoạn mặc định.

---

## 5. Danh Sách REST API

Tất cả các API trả về phản hồi theo định dạng thống nhất:
```json
{
  "status": 200,
  "message": "Mô tả kết quả thao tác",
  "data": { ... },
  "timestamp": "2026-08-24T19:30:00"
}
```

| Phương thức | Đường dẫn API | Mô tả |
|---|---|---|
| `POST` | `/api/orders` | Tạo đơn hàng mới từ văn bản mô tả, tự động phân tích AI và khởi tạo 6 công đoạn |
| `GET` | `/api/orders` | Lấy danh sách tất cả đơn hàng kèm thông số phân tích |
| `GET` | `/api/orders/{id}` | Lấy thông tin chi tiết đơn hàng và danh sách mẻ sản xuất |
| `GET` | `/api/batches` | Lấy danh sách tất cả mẻ sản xuất |
| `GET` | `/api/batches/{id}` | Lấy chi tiết mẻ sản xuất và lịch sử các công đoạn |
| `PATCH` | `/api/batches/{id}/advance` | Chuyển công đoạn mẻ sản xuất sang bước tiếp theo |
| `POST` | `/api/qc` | Nhập kết quả kiểm định QC (tự động phát hiện lỗi vượt ngưỡng 3% để cảnh báo) |
| `GET` | `/api/qc/batch/{batchId}` | Lấy lịch sử kiểm định QC của một mẻ sản xuất |
| `GET` | `/api/dashboard/stats` | Lấy các chỉ số thống kê tổng quan xưởng gốm |
| `GET` | `/api/dashboard/kanban` | Lấy dữ liệu bảng Kanban tiến độ sản xuất theo công đoạn |
| `POST` | `/api/telegram/webhook` | Webhook tiếp nhận thao tác bấm nút duyệt tiến độ từ Telegram |

---

## 6. Hướng Dẫn Chạy & Kiểm Thử

### Chạy kiểm thử tự động (Unit & Integration Tests)
```bash
cd c:\langgom\ceramics-backend
mvn clean test
```

### Khởi chạy ứng dụng Backend
```bash
mvn spring-boot:run
```
Trang tra cứu API Swagger UI hoạt động tại: `http://localhost:8080/swagger-ui.html`
