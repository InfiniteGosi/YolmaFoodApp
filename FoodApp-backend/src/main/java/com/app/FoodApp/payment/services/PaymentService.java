package com.app.FoodApp.payment.services;

import com.app.FoodApp.enums.PaymentStatus;
import com.app.FoodApp.payment.dtos.PaymentDTO;
import com.app.FoodApp.response.Response;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PaymentService {
    Response<?> initializePayment(PaymentDTO paymentDTO);
    void updatePaymentForOrder(PaymentDTO paymentDTO);
    Response<Page<PaymentDTO>> getAllPayments(PaymentStatus paymentStatus, int page, int size);
    Response<PaymentDTO> getPaymentById(Long id);
}
