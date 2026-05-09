package com.securepay.notification.repository;

import com.securepay.notification.model.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEvent, Long> {
    List<NotificationEvent> findByUserId(Long userId);
}
