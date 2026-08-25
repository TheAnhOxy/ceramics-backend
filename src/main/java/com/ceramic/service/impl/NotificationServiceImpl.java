package com.ceramic.service.impl;

import com.ceramic.entity.Alert;
import com.ceramic.entity.Batch;
import com.ceramic.entity.Stage;
import com.ceramic.enums.AlertType;
import com.ceramic.enums.Channel;
import com.ceramic.enums.DeliveryStatus;
import com.ceramic.integration.SlackClient;
import com.ceramic.integration.ZaloClient;
import com.ceramic.repository.AlertRepository;
import com.ceramic.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SlackClient slackClient;
    private final ZaloClient zaloClient;
    private final AlertRepository alertRepository;

    @Async
    @Override
    public void sendStageCompletedAlert(Batch batch, Stage completedStage, Stage nextStage) {
        try {
            String nextStageName = (nextStage != null) ? nextStage.getName() : "Hoàn tất toàn bộ quy trình";
            String text = String.format("THÔNG BÁO TIẾN ĐỘ XƯỞNG GỐM\n\nMẻ gốm #%s đã hoàn thành công đoạn: %s.\nCông đoạn tiếp theo: %s",
                    batch.getBatchCode(),
                    completedStage != null ? completedStage.getName() : "Khởi tạo",
                    nextStageName);

            String callbackData = (nextStage != null) ? "confirm_stage:" + batch.getId() : null;
            String buttonText = (nextStage != null) ? "Xác nhận hoàn thành " + nextStage.getName() : null;

            // Send to Slack & Zalo
            boolean slackSent = slackClient.sendMessage(text, callbackData, buttonText);
            boolean zaloSent = zaloClient.sendMessage(text, callbackData, buttonText);

            Alert alert = new Alert();
            alert.setBatch(batch);
            alert.setAlertType(AlertType.INFO);
            alert.setChannel(Channel.SLACK);
            alert.setMessage(text);
            alert.setDeliveryStatus((slackSent || zaloSent) ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
            alert.setSentAt(LocalDateTime.now());
            alertRepository.save(alert);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo chuyển công đoạn qua Slack/Zalo: {}", e.getMessage());
        }
    }

    @Async
    @Override
    public void sendCriticalAlert(Long batchId, String alertText) {
        try {
            String text = String.format("CẢNH BÁO SỰ CỐ KHẨN CẤP (RED ALERT)\n\n%s", alertText);

            boolean slackSent = slackClient.sendMessage(text, null, null);
            boolean zaloSent = zaloClient.sendMessage(text, null, null);

            Alert alert = new Alert();
            alert.setAlertType(AlertType.CRITICAL);
            alert.setChannel(Channel.SLACK);
            alert.setMessage(text);
            alert.setDeliveryStatus((slackSent || zaloSent) ? DeliveryStatus.SENT : DeliveryStatus.FAILED);
            alert.setSentAt(LocalDateTime.now());
            alertRepository.save(alert);
        } catch (Exception e) {
            log.error("Lỗi khi gửi cảnh báo sự cố khẩn cấp qua Slack/Zalo: {}", e.getMessage());
        }
    }
}
