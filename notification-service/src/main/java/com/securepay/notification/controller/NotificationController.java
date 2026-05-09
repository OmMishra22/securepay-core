package com.securepay.notification.controller;

import com.securepay.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<String> notify(@RequestBody Map<String, Object> payload,
                                         @RequestHeader("X-Internal-Secret") String secret) {
        notificationService.validateSecret(secret);
        notificationService.sendNotification(payload);
        return ResponseEntity.ok("NOTIFICATION_RECEIVED");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<?>> getForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }
}
