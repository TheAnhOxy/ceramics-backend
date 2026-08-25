# HỆ THỐNG ĐIỀU PHỐI VÀ GIÁM SÁT QUY TRÌNH SẢN XUẤT XƯỞNG GỐM BÁT TRÀNG
## BÁO CÁO MÃ NGUỒN MÁY CHỦ BACKEND REST API (JAVA SPRING BOOT)

Tài liệu báo cáo chi tiết mã nguồn dịch vụ máy chủ Backend xây dựng bằng Java Spring Boot 3. Backend đóng vai trò trung tâm điều phối luồng dữ liệu tự động hóa (Automation Core), tích hợp AI Agent bóc tách thông số kỹ thuật đơn hàng chuẩn JSON, điều phối 6 công đoạn chế tác gốm sứ Bát Tràng, kiểm soát đồng thời Khóa bi quan (PESSIMISTIC_WRITE), tính toán ngưỡng lỗi QC và tự động phát bản tin thông báo 2 chiều qua ứng dụng Slack/Zalo.

---

## 1. ĐÁP ỨNG TRỌN VẸN 4 THÀNH PHẦN CỐT LÕI ĐẦU RẠNG CỦA ĐỀ BÀI

```
+-----------------------------------------------------------------------------------+
|                        MÔ HÌNH THỰC THI 4 THÀNH PHẦN CỐT LÕI                      |
|                                                                                   |
|  (1) FRONTEND WEB UI     <===> (2) AUTOMATION PIPELINE LOGIC                      |
|  - Màn hình Kanban 7 cột       - Khởi tạo 6 công đoạn tự động liên hoàn           |
|  - Form nhập liệu tự nhiên     - Khóa bi quan @Lock(PESSIMISTIC_WRITE)           |
|                                                                                   |
|  (3) LLM AI AGENT ENGINE <===> (4) TÍCH HỢP KÊNH CHAT (SLACK / ZALO)             |
|  - Bóc tách 10 trường JSON     - Bắn tin tiến độ khi xong từng công đoạn          |
|  - Thử lại 3 lần & Fallback    - Cảnh báo đỏ khẩn cấp khi QC lỗi > 3%             |
|                                - Nút bấm [✅ Xác nhận] 2 chiều ngay trong chat     |
+-----------------------------------------------------------------------------------+
```

### Thành Phần 1: Giao Diện Web (Frontend Integration)
- Cung cấp đầy đủ các đường dẫn REST API theo chuẩn `ResponseEntity<ApiResponse<T>>` phục vụ cho giao diện Web React 18:
  - `POST /api/orders`: Tiếp nhận văn bản mô tả tự nhiên và khởi chạy AI.
  - `GET /api/dashboard/kanban`: Cung cấp danh sách mẻ gốm chia theo 7 cột.
  - `PATCH /api/batches/{id}/advance`: Chuyển mẻ gốm sang trạm tiếp theo.
  - `POST /api/qc`: Nhận dữ liệu kiểm định QC và phát cảnh báo đỏ khẩn cấp.

### Thành Phần 2: Tự Động Hóa (Automation Logic)
- **Tự động khởi tạo 6 công đoạn liên hoàn**: Ngay khi tiếp nhận đơn hàng, hệ thống tự động sinh mẻ gốm `#GOM-YYYYMMDD-XX` và khởi tạo lịch sử 6 trạm chế tác:
  1. `Tạo hình mộc` (FORMING - Trạng thái: `IN_PROGRESS`)
  2. `Phơi sấy & Sửa mộc` (DRYING_TRIMMING - Trạng thái: `PENDING`)
  3. `Vẽ họa tiết` (PAINTING - Trạng thái: `PENDING`)
  4. `Tráng men` (GLAZING - Trạng thái: `PENDING`)
  5. `Vào lò nung` (FIRING - Trạng thái: `PENDING`)
  6. `Kiểm định chất lượng (QC) & Đóng gói` (QC_PACKAGING - Trạng thái: `PENDING`)
- **Tự động chuyển trạng thái**: Khi một trạm hoàn thành, hệ thống tự động đánh dấu hoàn thành trạm cũ và mở trạng thái `IN_PROGRESS` cho trạm kế tiếp.

### Thành Phần 3: Ứng Dụng AI (LLM / Agent Integration)
- **Prompt đóng vai Kỹ sư Xưởng gốm Bát Tràng**: Ép mô hình LLM trả về đúng Schema JSON chuẩn gồm 10 thông số: `product_name`, `pattern`, `height_cm`, `glaze_type`, `quantity`, `firing_temp_celsius`, `firing_duration_hours`, `estimated_clay_kg`, `priority_level` và `confidence_note`.
- **Xử lý ngoại lệ AI (Retry 3 lần & Fallback)**: Nếu AI trả về JSON sai cú pháp, dịch vụ `AiExtractionServiceImpl` tự động thử lại 3 lần. Nếu vẫn gián đoạn mạng ngoài, hệ thống dùng thuật toán Smart Fallback tại local trích xuất các thông số cơ bản để quy trình sản xuất không bao giờ bị tắc nghẽn.

### Thành Phần 4: Tích Hợp Kênh Chat (Slack / Zalo Automation)
- **Tự động bắn thông báo hoàn thành công đoạn**: Khi mẻ gốm chuyển bước, hệ thống tự động phát bản tin thông báo tiến độ về nhóm chat.
  - **Ví dụ thực tế**: `"Mẻ gốm #GOM-88 đã vào lò nung - nhiệt độ 1280°C, thời gian nung 24 giờ"`.
- **Cảnh báo sự cố khẩn cấp khi QC phát hiện lỗi**: Khi thợ QC kiểm định và phát hiện tỷ lệ lỗi vượt quá 3%.
  - **Ví dụ thực tế**: `"Công đoạn QC phát hiện 10 sản phẩm nứt men trên 100 sản phẩm kiểm tra (Tỷ lệ lỗi 10.0% > 3.0%) -> Bắn cảnh báo đỏ về nhóm chat để quản lý xưởng xử lý dừng lò kịp thời!"`.
- **Tích hợp nút bấm xác nhận 2 chiều ngay trong Chat (Điểm cộng)**: Nhóm chat Slack/Zalo nhận tin nhắn chứa nút bấm **`[ ✅ Xác nhận hoàn thành công đoạn ]`**. Thợ xưởng bấm trực tiếp trong Slack, Webhook `/api/slack/webhook` tiếp nhận callback, cập nhật mẻ gốm và trả về phản hồi tin nhắn ẩn `{"response_type": "ephemeral"}` xóa biểu tượng chờ trên Slack.

---

## 2. BẢNG TỔNG HỢP CÁC LOGIC NGHIỆP VỤ BACKEND

```
+-----------------------------------------------------------------------------------+
|                        BẢNG CHI TIẾT LOGIC XỬ LÝ NỘI BỘ BACKEND                   |
|                                                                                   |
| 1. LOGIC TIẾP NHẬN ĐƠN HÀNG (Order & AI Processing Logic):                        |
|    - Nhập mô tả tự nhiên -> Gọi AiExtractionService -> Nhận JSON 10 thông số      |
|    - Tính toán lượng đất sét = (Chiều cao * Số lượng * Hằng số co ngót) / 10     |
|    - Đánh giá Priority: Nếu thời gian giao <= 5 ngày -> HIGH/URGENT, ngược lại    |
|                                                                                   |
| 2. LOGIC ĐIỀU PHỐI MẺ GỐM (Batch Pipeline Logic):                                 |
|    - Áp dụng @Lock(PESSIMISTIC_WRITE) khóa bản ghi Batch trong MySQL              |
|    - Kiểm tra thứ tự trạm: Không cho phép nhảy cóc công đoạn                      |
|    - Ghi nhận BatchStageHistory: Lưu thời gian, người thực hiện, ghi chú          |
|                                                                                   |
| 3. LOGIC KIỂM ĐỊNH QC & ĐÁNH GIÁ NGƯỠNG LỖI (QC & Threshold Alert Logic):         |
|    - Defect Rate (%) = (failedCount / totalChecked) * 100                         |
|    - Khống chế ngưỡng lỗi gốm sứ tinh xảo Bát Tràng = 3.0%                        |
|    - Nếu Defect Rate > 3.0%: Gán isCritical = true -> Lưu Alert Entity            |
|      -> Kích hoạt @Async NotificationService bắn tin CẢNH BÁO ĐỎ sang Chat       |
|                                                                                   |
| 4. LOGIC CHATBOT & WEBHOOK CALLBACK (ChatOps 2-Way Logic):                        |
|    - Endpoint POST /api/slack/webhook xử lý callback từ nút bấm                   |
|    - Phân tích payload -> Lấy batchId -> Gọi advanceStage()                       |
|    - Phản hồi JSON Ephemeral {"response_type": "ephemeral", "text": "✅ Thành công"}|
+-----------------------------------------------------------------------------------+
```

---

## 3. CẤU TRÚC MÃ NGUỒN MÁY CHỦ BACKEND

```
ceramics-backend/
├── pom.xml                               (Cấu hình dependencies: Spring Boot 3, JPA, MySQL, Jackson)
└── src/
    ├── main/java/com/ceramic/
    │   ├── CeramicPipelineApplication.java(File chạy chính Spring Boot)
    │   ├── config/                       (AsyncConfig, WebMvcConfig, JacksonConfig)
    │   ├── controller/                   (OrderController, BatchController, QcController, SlackWebhookController)
    │   ├── dto/                          (OrderCreateRequest, OrderResponse, AiExtractionResultDto, QcRecordDto)
    │   ├── entity/                       (Order, AiExtraction, Batch, Stage, BatchStageHistory, QcRecord, Alert)
    │   ├── enums/                        (StageCode, PriorityLevel, OrderStatus, BatchStatus)
    │   ├── exception/                    (GlobalExceptionHandler xử lý ngoại lệ tập trung)
    │   ├── integration/                  (LlmClient, LlmClientImpl, SlackIntegrationClient)
    │   ├── repository/                   (OrderRepository, BatchRepository với @Lock PESSIMISTIC_WRITE)
    │   └── service/                      (AiExtractionService, PipelineService, QcService, NotificationService)
    └── test/java/com/ceramic/            (Bộ kiểm thử tự động Unit Test & Integration Test)
```

---

## 4. DANH SÁCH REST API CONTRACTS

Định dạng phản hồi API chuẩn `ResponseEntity<ApiResponse<T>>`:
```json
{
  "status": 200,
  "message": "Thao tác thực hiện thành công",
  "data": { ... },
  "timestamp": "2026-08-25T17:40:00"
}
```

| HTTP Method | API Endpoint | Chức năng nghiệp vụ Backend |
|---|---|---|
| `POST` | `/api/orders` | Nhập câu văn tự nhiên, gọi AI bóc tách JSON và tự động tạo mẻ gốm |
| `GET` | `/api/orders` | Lấy danh sách tất cả đơn hàng kèm thông số phân tích AI |
| `GET` | `/api/orders/{id}` | Lấy chi tiết đơn hàng, mẻ gốm và lịch sử 6 công đoạn |
| `GET` | `/api/batches` | Lấy danh sách các mẻ gốm đang chế tác tại xưởng |
| `GET` | `/api/batches/{id}` | Lấy chi tiết mẻ gốm và lịch sử di chuyển qua các trạm |
| `PATCH` | `/api/batches/{id}/advance` | Chuyển mẻ gốm sang công đoạn sản xuất tiếp theo (Có khóa bi quan) |
| `POST` | `/api/qc` | Nhập kết quả kiểm định QC, tính % lỗi và phát thông báo đỏ nếu lỗi > 3% |
| `GET` | `/api/qc/batch/{batchId}` | Truy vấn lịch sử kiểm định chất lượng của một mẻ gốm |
| `GET` | `/api/dashboard/stats` | Tổng hợp chỉ số KPI (Tổng đơn, mẻ đang làm, mẻ xong, % QC đạt, số sự cố) |
| `GET` | `/api/dashboard/kanban` | Cung cấp danh sách các mẻ gốm nhóm theo 7 cột công đoạn |
| `POST` | `/api/slack/webhook` | Webhook tiếp nhận nút bấm xác nhận từ Slack/Zalo và phản hồi tức thì |

---

## 5. HƯỚNG DẪN KHỞI CHẠY DỰ ÁN BACKEND (SETUP GUIDE)

### Yêu Cầu Môi Trường Máy Chủ
- Java Development Kit (JDK) 17 trở lên.
- Apache Maven 3.8 trở lên.
- MySQL Server 8.0 trở lên (Hoặc tự động chạy chế độ H2 In-Memory).

### Bước 1: Mở Cửa Sổ Dòng Lệnh
Mở PowerShell hoặc Command Prompt và di chuyển vào thư mục backend:
```bash
cd ceramics-backend
```

### Bước 2: Khởi Chạy Kiểm Thử Tự Động (Unit & Integration Tests)
Chạy toàn bộ bộ kiểm thử để kiểm tra logic bóc tách AI, chuyển công đoạn và ngưỡng QC lỗi 3%:
```bash
mvn clean test
```

### Bước 3: Khởi Chạy Server Backend
Thực hiện lệnh Maven để biên dịch và mở dịch vụ Spring Boot:
```bash
mvn spring-boot:run
```
Sau khi dòng chữ thông báo khởi chạy hiện ra:
- **Server API**: Sẵn sàng phục vụ tại địa chỉ `http://localhost:8080`
- **Swagger UI Tra Cứu API**: Mở trình duyệt truy cập `http://localhost:8080/swagger-ui.html`

