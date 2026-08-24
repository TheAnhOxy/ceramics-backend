package com.ceramic.service;

import com.ceramic.dto.DashboardStatsResponse;
import com.ceramic.dto.KanbanBoardResponse;

public interface DashboardService {
    DashboardStatsResponse getDashboardStats();
    KanbanBoardResponse getKanbanBoard();
}
