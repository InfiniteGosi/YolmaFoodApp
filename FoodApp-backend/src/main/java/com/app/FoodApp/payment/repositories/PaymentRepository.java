package com.app.FoodApp.payment.repositories;

import com.app.FoodApp.enums.PaymentStatus;
import com.app.FoodApp.payment.entities.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);
}
