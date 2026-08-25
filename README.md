# HỆ THỐNG ĐIỀU PHỐI VÀ GIÁM SÁT QUY TRÌNH SẢN XUẤT XƯỞNG GỐM BÁT TRÀNG

## 1. GIỚI THIỆU BÀI TOÁN THỰC TẾ XƯỞNG GỐM BÁT TRÀNG

### Bối Cảnh Nghiệp Vụ Thực Tế
Xưởng gốm sứ Bát Tràng tiếp nhận hàng trăm đơn hàng gia công chế tác gốm sứ thủ công và công nghiệp mỗi tháng. Các đơn hàng gửi về từ các đại lý, nhà hàng, khách sạn thường ở dạng câu văn mô tả tự nhiên (Ví dụ: *"Đơn 500 Bộ ấm trà tử sa họa tiết men rạn cổ cao 18cm, nung lò 1250°C trong 20 giờ, giao gấp trong 7 ngày"*).

### Các Thách Thức Cần Giải Quyết
1. **Khó khăn trong tính toán thông số**: Quản lý xưởng mất nhiều thời gian đọc câu văn tự nhiên để tự tính thủ công lượng đất sét cần dùng, nhiệt độ lò nung, thời gian nung và độ ưu tiên tiến độ.
2. **Theo dõi tiến độ 6 công đoạn liên hoàn**: Quy trình sản xuất gốm sứ bắt buộc phải trải qua 6 trạm nối tiếp (*Tạo hình mộc $\rightarrow$ Phơi sấy & Sửa mộc $\rightarrow$ Vẽ họa tiết $\rightarrow$ Tráng men $\rightarrow$ Vào lò nung $\rightarrow$ QC & Đóng gói*). Việc theo dõi tiến độ thực tế của từng mẻ gốm tại các trạm nếu làm trên sổ sách thủ công rất dễ thất thoát và nhầm lẫn.
3. **Cảnh báo sự cố trễ**: Trong sản xuất gốm sứ tinh xảo, tỷ lệ nứt phôi mộc hoặc nứt men tối đa cho phép chỉ là 3%. Nếu sự cố nứt men ở lò nung xảy ra (tỷ lệ lỗi > 3%) mà không phát hiện kịp thời sẽ gây lãng phí rất lớn về chi phí nguyên liệu và công sức thợ.

### Mục Tiêu Giải Pháp Của Backend
- Xây dựng máy chủ Spring Boot tập trung xử lý tự động hóa luồng nghiệp vụ.
- Tích hợp mô-đun AI Agent bóc tách câu văn tự nhiên thành 10 thông số kỹ thuật chuẩn JSON.
- Tự động tạo mẻ gốm và gán lịch sử 6 công đoạn liên hoàn.
- Áp dụng Khóa bi quan (`PESSIMISTIC_WRITE`) chống tranh chấp dữ liệu khi thợ xưởng chuyển bước.
- Tính toán tỷ lệ lỗi QC thời gian thực và tự động phát bản tin CẢNH BÁO ĐỎ khẩn cấp 2 chiều sang Slack/Zalo để quản lý xưởng dừng lò xử lý kịp thời.

---

## 2. MÔ HÌNH VÀ CÁC THÀNH PHẦN CHÍNH CỦA MÁY CHỦ BACKEND

```
+-----------------------------------------------------------------------------------+
|                        MÔ HÌNH KIẾN TRÚC MÁY CHỦ BACKEND                          |
|                                                                                   |
|  (1) KẾT NỐI REST API WEB  <===> (2) LOGIC TỰ ĐỘNG HÓA CÔNG ĐOẠN                  |
|  - Cung cấp Endpoints JSON     - Khởi tạo 6 trạm chế tác liên hoàn                |
|  - Trả về ApiResponse chuẩn    - Khóa bi quan @Lock(PESSIMISTIC_WRITE)            |
|                                                                                   |
|  (3) AI AGENT ENGINE LLM   <===> (4) TÍCH HỢP KÊNH CHAT (SLACK / ZALO)              |
|  - Trích xuất 10 trường JSON   - Bắn tin tiến độ khi xong công đoạn               |
|  - Thử lại 3 lần & Fallback    - Cảnh báo đỏ khẩn cấp khi QC lỗi > 3%             |
|                                - Nút bấm [Xác nhận hoàn thành] 2 chiều            |
+-----------------------------------------------------------------------------------+
```

### Khối 1: Dịch Vụ REST API Cho Giao Diện Web
- Máy chủ cung cấp các đường dẫn REST API định dạng chuẩn `ResponseEntity<ApiResponse<T>>` phục vụ cho giao diện Web React 18:
  - `POST /api/orders`: Tiếp nhận văn bản mô tả tự nhiên và khởi chạy dịch vụ AI phân tích.
  - `GET /api/dashboard/kanban`: Cung cấp danh sách mẻ gốm phân chia theo 7 cột công đoạn.
  - `PATCH /api/batches/{id}/advance`: Cập nhật chuyển mẻ gốm sang trạm tiếp theo.
  - `POST /api/qc`: Nhận dữ liệu kiểm định chất lượng và kích hoạt cảnh báo đỏ nếu phát hiện sự cố.

### Khối 2: Logic Tự Động Hóa Quy Trình 6 Công Đoạn
- **Tự động khởi tạo 6 công đoạn liên hoàn**: Ngay khi tạo đơn hàng mới, hệ thống tự động sinh mẻ gốm `#GOM-YYYYMMDD-XX` và gán lịch sử 6 trạm chế tác:
  1. `Tạo hình mộc` (FORMING - Trạng thái: `IN_PROGRESS`)
  2. `Phơi sấy & Sửa mộc` (DRYING_TRIMMING - Trạng thái: `PENDING`)
  3. `Vẽ họa tiết` (PAINTING - Trạng thái: `PENDING`)
  4. `Tráng men` (GLAZING - Trạng thái: `PENDING`)
  5. `Vào lò nung` (FIRING - Trạng thái: `PENDING`)
  6. `Kiểm định chất lượng (QC) & Đóng gói` (QC_PACKAGING - Trạng thái: `PENDING`)
- **Tự động chuyển trạng thái**: Khi một trạm hoàn thành, hệ thống tự động lưu vết thời gian, cập nhật trạm cũ và mở trạng thái thực hiện cho trạm kế tiếp.

### Khối 3: Dịch Vụ AI Agent Phân Tích Thông Số JSON
- **Prompt thiết kế chuẩn chuyên môn gốm Bát Tràng**: Ép mô hình LLM phân tích câu văn tự nhiên và trả về cấu trúc JSON 10 thông số: `product_name`, `pattern`, `height_cm`, `glaze_type`, `quantity`, `firing_temp_celsius`, `firing_duration_hours`, `estimated_clay_kg`, `priority_level` và `confidence_note`.
- **Xử lý ngoại lệ (Retry 3 lần & Fallback)**: Nếu phản hồi AI bị lỗi định dạng, dịch vụ `AiExtractionServiceImpl` tự động thử lại 3 lần. Nếu mất mạng ngoài, hệ thống tự động dùng thuật toán dự phòng tại local để tiếp nhận đơn hàng không bị gián đoạn.

### Khối 4: Dịch Vụ Thông Báo & Tương Tác 2 Chiều Slack / Zalo
- **Tự động gửi thông báo hoàn thành công đoạn**: Khi mẻ gốm chuyển bước, hệ thống tự động phát bản tin thông báo tiến độ về nhóm chat Slack/Zalo.
  - *Ví dụ thực tế*: `"Mẻ gốm #GOM-88 đã vào lò nung - nhiệt độ 1280°C, thời gian nung 24 giờ"`.
- **Cảnh báo sự cố khẩn cấp khi QC phát hiện lỗi**: Khi thợ QC kiểm định phát hiện tỷ lệ lỗi vượt quá 3%.
  - *Ví dụ thực tế*: `"Công đoạn QC phát hiện 10 sản phẩm nứt men trên 100 sản phẩm kiểm tra (Tỷ lệ lỗi 10.0% > 3.0%) -> Bắn cảnh báo đỏ về nhóm chat để quản lý xưởng xử lý dừng lò kịp thời!"`.
- **Nút bấm xác nhận 2 chiều ngay trong Chat**: Nhóm chat Slack/Zalo nhận tin nhắn chứa nút bấm `[ Xác nhận hoàn thành công đoạn ]`. Thợ xưởng bấm trực tiếp trong Slack, Webhook `/api/slack/webhook` tiếp nhận callback, cập nhật mẻ gốm và trả về phản hồi tin nhắn ẩn `{"response_type": "ephemeral"}` xóa biểu tượng chờ trên Slack.

---

## 3. CHI TIẾT LOGIC XỬ LÝ TỰ ĐỘNG VÀ NGOẠI LỆ (CORE LOGIC & ERROR HANDLING)

```
+-----------------------------------------------------------------------------------+
|                  CHI TIẾT LUỒNG XỬ LÝ TỰ ĐỘNG VÀ XỬ LÝ LỖI (CORE LOGIC)           |
|                                                                                   |
|  1. THIẾT KẾ PROMPT & BÓC TÁCH SCHEMA JSON AI AGENT:                              |
|     - System Prompt ép LLM trả về đúng JSON Schema 10 thông số kỹ thuật gốm sứ.   |
|     - Tự động tính toán lượng đất sét = (Chiều cao * Số lượng * Hằng số ngót) / 10 |
|     - Tự động đánh giá Priority: Hạn giao <= 5 ngày -> URGENT/HIGH, > 5 ngày -> NORMAL|
|                                                                                   |
|  2. XỬ LÝ LUỒNG TỰ ĐỘNG END-TO-END VÀ ĐỘ ỔN ĐỊNH DỮ LIỆU:                        |
|     - Áp dụng @Lock(PESSIMISTIC_WRITE) khóa bản ghi Batch trong MySQL Database    |
|     - Ngăn chặn triệt để lỗi tranh chấp đồng thời (Race Condition) khi nhiều thợ   |
|       hoặc quản lý trên Slack cùng bấm nút chuyển bước một lúc.                  |
|     - Ràng buộc luồng 6 trạm: Ngăn nhảy cóc công đoạn, kiểm tra trạng thái hợp lệ |
|                                                                                   |
|  3. XỬ LÝ CÁC TÌNH HUỐNG LỖI VÀ NGOẠI LỆ (ERROR & EXCEPTION HANDLING):           |
|     - Ngoại lệ AI JSON sai cấu trúc -> Vòng lặp thử lại 3 lần (Retry 3x Loop)     |
|     - Mất kết nối API ngoài -> Kích hoạt Smart Fallback Engine trích xuất tại local|
|     - Gửi tin nhắn Slack trễ mạng -> Chạy bất đồng bộ ngầm @Async ThreadPool      |
|     - Lỗi tham số / Không tìm thấy ID -> GlobalExceptionHandler xử lý chuẩn HTTP 400/404|
+-----------------------------------------------------------------------------------+
```

### Chi Tiết Kỹ Thuật Logic Xử Lý Và Xử Lý Lỗi Nội Bộ Backend:

1. **Thiết kế Prompt AI và Đảm bảo Schema JSON chuẩn**:
   - System Prompt đóng vai Kỹ sư Xưởng gốm Bát Tràng, quy định chặt chẽ các kiểu dữ liệu trả về (`height_cm` dạng number, `glaze_type` dạng string, `priority_level` dạng enum `URGENT/HIGH/NORMAL`).
   - Tự động bổ sung các thông số ước tính như lượng đất sét (kg) và ghi chú tin cậy giúp quản lý xưởng đưa ra quyết định sản xuất chính xác.

2. **Cơ chế Thử lại 3 lần (Retry 3x) & Smart Fallback Engine**:
   - Khi dịch vụ `AiExtractionServiceImpl` nhận phản hồi từ LLM API, hệ thống chạy qua bộ kiểm tra cấu trúc `validateAndParseJson()`.
   - Nếu AI trả về chuỗi văn bản tự do hoặc hỏng dấu đóng mở ngoặc, hàm bắt ngoại lệ `JsonParseException` và kích hoạt vòng lặp gửi lại request với prompt nhắc lại cấu trúc chuẩn tối đa 3 lần.
   - Nếu xảy ra sự cố sập mạng hoặc hết quota API ngoài, hệ thống dùng bộ phân tích regex dự phòng tại local để trích xuất tên sản phẩm và số lượng, đảm bảo ứng dụng không bao giờ bị đứng hoặc văng lỗi ra giao diện.

3. **Kiểm soát đồng thời bằng Khóa bi quan (`@Lock(PESSIMISTIC_WRITE)`)**:
   - Khi API `PATCH /api/batches/{id}/advance` hoặc Webhook `/api/slack/webhook` tiếp nhận yêu cầu chuyển bước cho mẻ gốm, Repository thực thi câu lệnh SQL `SELECT ... FOR UPDATE`.
   - Giao dịch thứ hai cố tình thao tác cùng một mẻ gốm sẽ phải chờ giao dịch đầu tiên commit thành công. Điều này bảo vệ tuyệt đối dữ liệu lịch sử công đoạn (`BatchStageHistory`) không bị ghi đè hoặc sai lệch trạng thái.

4. **Xử lý bất đồng bộ ngầm `@Async` cho Dịch vụ Thông báo**:
   - Việc gửi tin nhắn HTTP POST sang Webhook của Slack/Zalo được thực thi trong một luồng riêng (`ThreadPoolTaskExecutor`).
   - Nếu mạng Slack/Zalo bị trễ (latency cao), người dùng trên giao diện Web vẫn nhận được phản hồi ngay lập tức mà không bị xoay vòng chờ đợi.

5. **Xử lý ngoại lệ tập trung (`GlobalExceptionHandler`)**:
   - Sử dụng `@RestControllerAdvice` để bắt toàn bộ các ngoại lệ `ResourceNotFoundException`, `IllegalArgumentException`, `MethodArgumentNotValidException`.
   - Mọi lỗi văng ra từ hệ thống đều được bọc lại theo cấu trúc JSON chuẩn `ApiResponse(status, message, data, timestamp)` giúp Frontend nhận biết nguyên nhân lỗi chính xác.

---

## 4. CẤU TRÚC MÃ NGUỒN MÁY CHỦ BACKEND

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

## 5. DANH SÁCH REST API CONTRACTS

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

## 6. HƯỚNG DẪN KHỞI CHẠY DỰ ÁN BACKEND (SETUP GUIDE)

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
