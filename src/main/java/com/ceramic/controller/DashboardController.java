package com.ceramic.controller;

import com.ceramic.dto.ApiResponse;
import com.ceramic.dto.DashboardStatsResponse;
import com.ceramic.dto.KanbanBoardResponse;
import com.ceramic.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        log.info("Lấy thông số thống kê Dashboard");
        DashboardStatsResponse stats = dashboardService.getDashboardStats();

        ApiResponse<DashboardStatsResponse> response = ApiResponse.<DashboardStatsResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thống kê Dashboard thành công")
                .data(stats)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/kanban")
    public ResponseEntity<ApiResponse<KanbanBoardResponse>> getKanbanBoard() {
        log.info("Lấy dữ liệu Bảng Kanban xưởng gốm");
        KanbanBoardResponse kanban = dashboardService.getKanbanBoard();

        ApiResponse<KanbanBoardResponse> response = ApiResponse.<KanbanBoardResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy bảng Kanban tiến độ sản xuất thành công")
                .data(kanban)
                .build();
        return ResponseEntity.ok(response);
    }
}
