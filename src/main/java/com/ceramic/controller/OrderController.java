package com.ceramic.controller;

import com.ceramic.dto.ApiResponse;
import com.ceramic.dto.OrderCreateRequest;
import com.ceramic.dto.OrderResponse;
import com.ceramic.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        log.info("Khởi tạo đơn hàng mới với mô tả: {}", request.getRawDescription());
        OrderResponse result = orderService.createOrder(request);

        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tiếp nhận đơn hàng và trích xuất AI tự động thành công")
                .data(result)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        log.info("Lấy danh sách tất cả đơn hàng");
        List<OrderResponse> orders = orderService.getAllOrders();

        ApiResponse<List<OrderResponse>> response = ApiResponse.<List<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách đơn hàng thành công")
                .data(orders)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        log.info("Chi tiết đơn hàng ID: {}", id);
        OrderResponse order = orderService.getOrderById(id);

        ApiResponse<OrderResponse> response = ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy chi tiết đơn hàng thành công")
                .data(order)
                .build();
        return ResponseEntity.ok(response);
    }
}
