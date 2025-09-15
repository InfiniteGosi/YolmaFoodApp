package com.app.FoodApp;

import com.app.FoodApp.emailNofitication.dtos.NotificationDTO;
import com.app.FoodApp.emailNofitication.services.NotificationService;
import com.app.FoodApp.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FoodAppApplication {
	public static void main(String[] args) {
		SpringApplication.run(FoodAppApplication.class, args);
	}
}
