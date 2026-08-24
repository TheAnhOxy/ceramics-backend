package com.ceramic.service;

import com.ceramic.dto.OrderCreateRequest;
import com.ceramic.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderCreateRequest request);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getAllOrders();
}
