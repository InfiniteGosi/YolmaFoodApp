package com.app.FoodApp.emailNofitication.repositories;

import com.app.FoodApp.emailNofitication.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
