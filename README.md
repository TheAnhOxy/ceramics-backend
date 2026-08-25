# HỆ THỐNG ĐIỀU PHỐI VÀ GIÁM SÁT QUY TRÌNH SẢN XUẤT XƯỞNG GỐM BÁT TRÀNG
## DỊCH VỤ MÁY CHỦ BACKEND REST API (JAVA SPRING BOOT)

Mã nguồn dịch vụ máy chủ Backend xây dựng bằng Java Spring Boot 3, đóng vai trò trung tâm xử lý dữ liệu, bóc tách thông số đơn hàng thông minh bằng AI Agent, điều phối trạng thái 6 công đoạn chế tác gốm sứ Bát Tràng, tính toán tỷ lệ lỗi kiểm định QC và xử lý callback 2 chiều thời gian thực qua ứng dụng làm việc Slack/Zalo.

---

## I. GIỚI THIỆU BÀI TOÁN KỸ THUẬT VÀ GIẢI PHÁP BACKEND

### 1. Bài Toán Thực Tế Tại Xưởng Gốm Bát Tràng
- **Tiếp nhận đơn hàng không chuẩn hóa**: Các yêu cầu đơn hàng từ khách hàng thường ở dạng câu văn tự nhiên (Ví dụ: *"Đơn 500 Bộ ấm trà tử sa họa tiết men rạn cổ cao 18cm, nung lò 1250°C trong 20 giờ, giao gấp trong 7 ngày"*), gây khó khăn cho việc nhập liệu thủ công.
- **Xung đột điều phối đồng thời (Race Condition)**: Tại xưởng gốm, nhiều thợ tại các trạm công đoạn khác nhau hoặc quản lý thao tác qua nút bấm Slack/Zalo có thể chuyển bước cho cùng một mẻ gốm tại cùng một thời điểm.
- **Cảnh báo sự cố trễ**: Tỷ lệ hỏng/nứt mộc khi phơi sấy hoặc nung lò nếu vượt quá mức an toàn (3%) nếu không được phát hiện kịp thời sẽ gây thiệt hại chi phí nguyên liệu rất lớn.

### 2. Giải Pháp Kiến Trúc Backend
- **Tích hợp LLM AI Agent bóc tách 10 trường JSON**: Tự động chuyển đổi câu văn mô tả tự nhiên thành dữ liệu kỹ thuật có cấu trúc bao gồm tên sản phẩm, họa tiết, loại men, chiều cao, số lượng, nhiệt độ nung, thời gian nung, ước tính lượng đất sét, mức ưu tiên và ghi chú tin cậy.
- **Kiểm soát đồng thời bằng Khóa Bi Quan (`PESSIMISTIC_WRITE`)**: Khóa bản ghi mẻ gốm trong MySQL trong suốt quá trình giao dịch xử lý chuyển công đoạn, triệt tiêu hoàn toàn rủi ro xung đột dữ liệu.
- **Xử lý bất đồng bộ ngầm (`@Async`)**: Gửi bản tin cảnh báo khẩn cấp và cập nhật tiến độ sang Slack/Zalo ở luồng phụ (Thread Pool), đảm bảo thời gian phản hồi API (Response Time) < 100ms.

---

## II. KIẾN TRÚC MÃ NGUỒN VÀ MÔ HÌNH DỮ LIỆU BACKEND

```
+-----------------------------------------------------------------------------------+
|                        MÔ HÌNH SƠ ĐỒ LỚP VÀ DÒNG CHẢY DỮ LIỆU                     |
|                                                                                   |
|  [Client Web / Webhook] ---> [Controller Layer] ---> [Service Layer]             |
|                                                            |                      |
|                                   +------------------------+-------------------+  |
|                                   |                        |                   |  |
|                                   v                        v                   v  |
|                            [AiExtractionService]   [PipelineService]     [QcService]|
|                            (LlmClient & Retry)     (Pessimistic Lock)    (Defect 3%)|
|                                   |                        |                   |  |
|                                   +------------------------+-------------------+  |
|                                                            |                      |
|                                                            v                      |
|                                                  [Repository & MySQL DB]          |
+-----------------------------------------------------------------------------------+
```

### 1. Phân Tầng Mã Nguồn (Package Structure)
```
ceramics-backend/
├── pom.xml                               (Cấu hình Maven, Spring Boot 3, JPA, MySQL, Jackson)
└── src/
    ├── main/java/com/ceramic/
    │   ├── CeramicPipelineApplication.java(Point khởi chạy ứng dụng Spring Boot)
    │   ├── config/                       (AsyncConfig, WebMvcConfig, JacksonConfig)
    │   ├── controller/                   (OrderController, BatchController, QcController, SlackWebhookController)
    │   ├── dto/                          (OrderCreateRequest, OrderResponse, AiExtractionResultDto, QcRecordDto)
    │   ├── entity/                       (Order, AiExtraction, Batch, Stage, BatchStageHistory, QcRecord, Alert)
    │   ├── enums/                        (StageCode, PriorityLevel, OrderStatus, BatchStatus)
    │   ├── exception/                    (GlobalExceptionHandler, ResourceNotFoundException)
    │   ├── integration/                  (LlmClient, LlmClientImpl, SlackIntegrationClient)
    │   ├── repository/                   (OrderRepository, BatchRepository, QcRecordRepository, AlertRepository)
    │   └── service/                      (AiExtractionService, PipelineService, QcService, NotificationService)
    └── test/java/com/ceramic/            (Bộ kiểm thử tự động Unit Test & Integration Test)
```

### 2. Các Thực Thể Cơ Sở Dữ Liệu JPA (Database Schema Entities)
- **`Order`**: Lưu thông tin đơn hàng, khách hàng, mô tả tự nhiên và trạng thái tổng.
- **`AiExtraction`**: Lưu 10 thông số bóc tách tự động bởi AI Agent liên kết 1-1 với Order.
- **`Batch`**: Lưu thông tin mẻ gốm sản xuất, mã mẻ `#GOM-YYYYMMDD-XX`, số lượng và trạm hiện tại.
- **`Stage`**: Định danh 6 công đoạn chế tác chuẩn Bát Tràng (FORMING, DRYING_TRIMMING, PAINTING, GLAZING, FIRING, QC_PACKAGING).
- **`BatchStageHistory`**: Lưu vết lịch sử di chuyển qua từng trạm (thời gian hoàn thành, người thực hiện, ghi chú).
- **`QcRecord`**: Lưu kết quả kiểm định QC (số lượng kiểm tra, số lỗi, % lỗi, cờ khẩn cấp `isCritical`).
- **`Alert`**: Lưu nhật ký cảnh báo đỏ gửi về hệ thống khi tỷ lệ lỗi QC > 3.0%.

---

## III. SƠ ĐỒ XỬ LÝ NGHIỆP VỤ THỜI GIAN THỰC (BACKEND DATA FLOW)

```
+-----------------------------------------------------------------------------------+
|                     SƠ ĐỒ TUẦN TỰ XỬ LÝ NỘI BỘ MÁY CHỦ BACKEND                   |
|                                                                                   |
|  1. POST /api/orders  ---> AiExtractionServiceImpl ---> LlmClient (Retry 3x)     |
|                                                              |                    |
|                                                              v                    |
|  2. Khởi tạo Batch    <--- Trích xuất JSON 10 thông số <-----+                    |
|     + Gán 6 Stage History Timeline (FORMING -> QC_PACKAGING)                      |
|                                                                                   |
|  3. POST /api/slack/webhook <--- Callback nút bấm Slack/Zalo                     |
|     ---> Lock Batch (@Lock PESSIMISTIC_WRITE) ---> Advance Stage                 |
|     ---> Trả về Ephemeral Response JSON {"response_type": "ephemeral"}            |
|                                                                                   |
|  4. POST /api/qc      ---> Tính Defect Rate = (failedCount / totalChecked) * 100  |
|     ---> Nếu Defect Rate > 3.0%                                                   |
|          + Set isCritical = true                                                  |
|          + @Async NotificationService.sendRedAlertToSlack()                       |
+-----------------------------------------------------------------------------------+
```

---

## IV. DANH SÁCH API CONTRACTS (RESTFUL ENDPOINTS)

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
| `POST` | `/api/orders` | Tiếp nhận đơn văn bản tự nhiên, gọi AI bóc tách JSON và tự động tạo mẻ gốm |
| `GET` | `/api/orders` | Tra cứu danh sách đơn hàng kèm dữ liệu thông số AI bóc tách |
| `GET` | `/api/orders/{id}` | Lấy chi tiết đơn hàng, mẻ gốm và lịch sử 6 công đoạn |
| `GET` | `/api/batches` | Lấy danh sách tất cả các mẻ gốm đang chế tác tại xưởng |
| `GET` | `/api/batches/{id}` | Lấy chi tiết mẻ gốm và lịch sử di chuyển qua các trạm |
| `PATCH` | `/api/batches/{id}/advance` | Chuyển mẻ gốm sang công đoạn sản xuất tiếp theo (Áp dụng Khóa bi quan) |
| `POST` | `/api/qc` | Lưu kết quả kiểm định QC, tính % lỗi và phát thông báo đỏ nếu lỗi > 3% |
| `GET` | `/api/qc/batch/{batchId}` | Truy vấn lịch sử kiểm định chất lượng của một mẻ gốm |
| `GET` | `/api/dashboard/stats` | Tổng hợp chỉ số KPI (Tổng đơn, mẻ đang làm, mẻ xong, % QC đạt, số sự cố) |
| `GET` | `/api/dashboard/kanban` | Cung cấp danh sách các mẻ gốm nhóm theo 7 cột công đoạn |
| `POST` | `/api/slack/webhook` | Tiếp nhận callback nút bấm tương tác từ Slack/Zalo và phản hồi tức thì |

---

## V. HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY DỰ ÁN BACKEND (SETUP GUIDE)

### Yêu Cầu Môi Trường Máy Chủ
- Java Development Kit (JDK) 17 trở lên.
- Apache Maven 3.8 trở lên.
- MySQL Server 8.0 trở lên (Mặc định tự động chạy H2 In-Memory nếu không cấu hình MySQL).

### 1. Di Chuyển Vào Thư Mục Backend
Mở Terminal / PowerShell và di chuyển vào thư mục dịch vụ backend:
```bash
cd ceramics-backend
```

### 2. Khởi Chạy Bộ Kiểm Thử Tự Động (Unit & Integration Tests)
Chạy toàn bộ bộ kiểm thử tự động để xác nhận logic bóc tách AI, chuyển công đoạn và tính toán ngưỡng QC 3%:
```bash
mvn clean test
```

### 3. Khởi Chạy Server Backend (Development Mode)
Thực hiện lệnh Maven để biên dịch và chạy dịch vụ Spring Boot:
```bash
mvn spring-boot:run
```
Sau khi dòng chữ `Started CeramicPipelineApplication` xuất hiện:
- **Server API**: Sẵn sàng phục vụ yêu cầu tại địa chỉ `http://localhost:8080`
- **Swagger UI Tra Cứu API**: Mở trình duyệt truy cập `http://localhost:8080/swagger-ui.html`
