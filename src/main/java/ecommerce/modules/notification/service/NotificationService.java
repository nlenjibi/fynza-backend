package ecommerce.modules.notification.service;

import ecommerce.modules.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> findByUser(UUID userId);

    NotificationResponse markAsRead(UUID id);

    void markAllAsRead(UUID userId);
}
