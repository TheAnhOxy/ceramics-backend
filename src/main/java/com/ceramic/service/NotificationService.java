package com.ceramic.service;

import com.ceramic.entity.Batch;
import com.ceramic.entity.Stage;

public interface NotificationService {
    void sendStageCompletedAlert(Batch batch, Stage completedStage, Stage nextStage);
    void sendCriticalAlert(Long batchId, String alertText);
}
