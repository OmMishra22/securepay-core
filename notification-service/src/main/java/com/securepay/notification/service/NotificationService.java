package com.securepay.notification.service;

import com.securepay.notification.exception.UnauthorizedException;
import com.securepay.notification.exception.ValidationException;
import com.securepay.notification.model.NotificationEvent;
import com.securepay.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final String internalSecret;

    public NotificationService(NotificationRepository notificationRepository, org.springframework.core.env.Environment environment) {
        this.notificationRepository = notificationRepository;
        this.internalSecret = environment.getProperty("INTERNAL_SECRET", "secret-key");
    }

    public void validateSecret(String secret) {
        if (secret == null || !internalSecret.equals(secret)) {
            logger.warn("Invalid internal secret received");
            throw new UnauthorizedException("Invalid internal secret");
        }
    }

    public void sendNotification(Map<String, Object> payload) {
        if (payload == null) {
            throw new ValidationException("Payload cannot be null");
        }

        Object userIdObj = payload.get("userId");
        Object messageObj = payload.get("message");

        if (userIdObj == null) {
            throw new ValidationException("userId is required");
        }
        if (messageObj == null || ((String) messageObj).isBlank()) {
            throw new ValidationException("message is required and cannot be blank");
        }

        Long userId;
        try {
            userId = ((Number) userIdObj).longValue();
        } catch (ClassCastException e) {
            throw new ValidationException("userId must be a valid number");
        }

        String message = (String) messageObj;
        NotificationEvent event = new NotificationEvent(userId, message);
        notificationRepository.save(event);
        logger.info("Notification saved: userId={} message={}", userId, message);
    }

    public List<NotificationEvent> getNotifications(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("userId must be greater than 0");
        }
        logger.info("Fetching notifications for userId={}", userId);
        return notificationRepository.findByUserId(userId);
    }
}
