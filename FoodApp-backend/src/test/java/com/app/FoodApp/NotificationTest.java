package com.app.FoodApp;

import com.app.FoodApp.emailNofitication.dtos.NotificationDTO;
import com.app.FoodApp.emailNofitication.repositories.NotificationRepository;
import com.app.FoodApp.emailNofitication.services.NotificationServiceImpl;
import com.app.FoodApp.enums.NotificationType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void shouldSendEmailAndSaveNotification() throws Exception {
        // Arrange
        NotificationDTO dto = NotificationDTO.builder()
                .recipient("test@example.com")
                .subject("Test Subject")
                .body("This is a test email")
                .type(NotificationType.EMAIL)
                .isHtml(false)
                .build();

        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        notificationService.sendEmail(dto);

        // Assert
        // verify mail sending
        verify(mailSender, times(1)).send(mimeMessage);

        // verify saving to repository
        verify(notificationRepository, times(1)).save(argThat(notification ->
                notification.getRecipient().equals(dto.getRecipient()) &&
                        notification.getSubject().equals(dto.getSubject()) &&
                        notification.getBody().equals(dto.getBody()) &&
                        notification.getType() == NotificationType.EMAIL &&
                        notification.isHtml() == dto.isHtml()
        ));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenMailFails() {
        // Arrange
        NotificationDTO dto = NotificationDTO.builder()
                .recipient("fail@example.com")
                .subject("Fail Subject")
                .body("This should fail")
                .type(NotificationType.EMAIL)
                .isHtml(false)
                .build();

        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail creation failed"));

        // Act + Assert
        assertThrows(RuntimeException.class, () -> notificationService.sendEmail(dto));

        // repository should never be called
        verify(notificationRepository, never()).save(any());
    }
}
