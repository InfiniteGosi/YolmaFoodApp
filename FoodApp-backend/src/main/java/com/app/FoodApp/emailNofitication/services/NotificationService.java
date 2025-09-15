package com.app.FoodApp.emailNofitication.services;

import com.app.FoodApp.emailNofitication.dtos.NotificationDTO;

public interface NotificationService {
    void sendEmail(NotificationDTO notificationDTO);
}
