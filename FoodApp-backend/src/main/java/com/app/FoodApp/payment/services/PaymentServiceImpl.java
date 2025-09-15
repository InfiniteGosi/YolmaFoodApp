package com.app.FoodApp.payment.services;

import com.app.FoodApp.emailNofitication.dtos.NotificationDTO;
import com.app.FoodApp.emailNofitication.services.NotificationService;
import com.app.FoodApp.enums.OrderStatus;
import com.app.FoodApp.enums.PaymentGateway;
import com.app.FoodApp.enums.PaymentStatus;
import com.app.FoodApp.exceptions.BadRequestException;
import com.app.FoodApp.exceptions.NotFoundException;
import com.app.FoodApp.order.entities.Order;
import com.app.FoodApp.order.repositories.OrderRepository;
import com.app.FoodApp.payment.dtos.PaymentDTO;
import com.app.FoodApp.payment.entities.Payment;
import com.app.FoodApp.payment.repositories.PaymentRepository;
import com.app.FoodApp.response.Response;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final TemplateEngine templateEngine;
    private final ModelMapper modelMapper;

    // Stripe API secret key (from application.properties or environment variables)
    @Value("${stripe.api.secret.key}")
    private String secretKey;

    // Base URL of the frontend app (used in email templates for links)
    @Value("${frontend.base.url}")
    private String frontendBaseUrl;

    /**
     * Initializes a Stripe payment for a given order.
     */
    @Override
    public Response<?> initializePayment(PaymentDTO paymentDTO) {
        Stripe.apiKey = secretKey; // Set Stripe API key

        Long orderId = paymentDTO.getOrderId();

        // Fetch order from database, throw exception if not found
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(("Order not found")));

        // Prevent multiple payments if already completed
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new BadRequestException("Payment is already completed");
        }

        // Validate that an amount is provided
        if (paymentDTO.getAmount() == null) {
            throw new BadRequestException("Amount is required");
        }

        // Ensure that the payment amount matches the order total
        if (order.getTotalAmount().compareTo(paymentDTO.getAmount()) != 0) {
            throw new BadRequestException("Payment amount is incorrect, please contact customer support");
        }

        try {
            // Build Stripe payment intent request
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(paymentDTO.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Convert to cents
                    .setCurrency("usd") // Fixed currency USD
                    .putMetadata("orderId", String.valueOf(orderId)) // Store orderId for traceability
                    .build();

            // Create Stripe payment intent
            PaymentIntent intent = PaymentIntent.create(params);
            String uniqueTransactionId = intent.getClientSecret(); // Transaction client secret

            // Return response with client secret
            return Response.builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Success")
                    .data(uniqueTransactionId)
                    .build();
        }
        catch (Exception ex) {
            // Catch-all for Stripe or API errors
            throw new RuntimeException("Error creating payment unique transaction id");
        }
    }

    /**
     * Updates payment status for an order after payment attempt (success/failure).
     */
    @Override
    public void updatePaymentForOrder(PaymentDTO paymentDTO) {
        Long orderId = paymentDTO.getOrderId();

        // Find order linked to payment
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(("Order not found")));

        // Create a new Payment record
        Payment payment = new Payment();
        payment.setPaymentGateway(PaymentGateway.STRIPE);
        payment.setAmount(paymentDTO.getAmount());
        payment.setTransactionId(paymentDTO.getTransactionId());
        payment.setPaymentStatus(paymentDTO.isSuccess() ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setOrder(order);
        payment.setUser(order.getUser());

        // Save failure reason if unsuccessful
        if (!paymentDTO.isSuccess()) {
            payment.setFailureReason(paymentDTO.getFailureReason());
        }

        // Persist payment to database
        paymentRepository.save(payment);

        // Prepare Thymeleaf email context
        Context context = new Context(Locale.getDefault());
        context.setVariable("customerName", order.getUser().getName());
        context.setVariable("orderId", order.getId());
        context.setVariable("currentYear", Year.now().getValue());
        context.setVariable("amount", "$" + paymentDTO.getAmount());

        if (paymentDTO.isSuccess()) {
            // If payment successful → update order to confirmed
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // Add extra info to success email
            context.setVariable("transactionId", paymentDTO.getTransactionId());
            context.setVariable("paymentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")));
            context.setVariable("frontendBaseUrl", this.frontendBaseUrl);

            // Generate success email template
            String emailBody = templateEngine.process("payment-success", context);

            // Send success notification
            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(order.getUser().getEmail())
                    .subject("Payment Successful - Order #" + order.getId())
                    .body(emailBody)
                    .isHtml(true)
                    .build());
        }
        else {
            // If payment failed → mark order as cancelled
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            // Add failure reason to failure email
            context.setVariable("failureReason", paymentDTO.getFailureReason());

            // Generate failure email template
            String emailBody = templateEngine.process("payment-failed", context);

            // Send failure notification
            notificationService.sendEmail(NotificationDTO.builder()
                    .recipient(order.getUser().getEmail())
                    .subject("Payment Failed - Order #" + order.getId())
                    .body(emailBody)
                    .isHtml(true)
                    .build());
        }
    }

    /**
     * Retrieves all payments from database, maps them to DTOs, and removes unnecessary references.
     */
    @Override
    public Response<Page<PaymentDTO>> getAllPayments(PaymentStatus paymentStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Payment> paymentPage;

        if (paymentStatus != null) {
            paymentPage = paymentRepository.findByPaymentStatus(paymentStatus, pageable);
        }
        else {
            paymentPage = paymentRepository.findAll(pageable);
        }

        Page<PaymentDTO> paymentDTOPage = paymentPage.map(payment -> {
            PaymentDTO paymentDTO = modelMapper.map(payment, PaymentDTO.class);
            paymentDTO.setOrder(null);
            paymentDTO.setUser(null);
            return paymentDTO;
        });

        // Return response with list of payments
        return Response.<Page<PaymentDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Payments retrieved successfully")
                .data(paymentDTOPage)
                .build();
    }

    /**
     * Retrieves a single payment by ID and returns a sanitized DTO.
     */
    @Override
    public Response<PaymentDTO> getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id).orElseThrow(()-> new NotFoundException("Payment not found"));
        PaymentDTO paymentDTOS = modelMapper.map(payment, PaymentDTO.class);
        
        paymentDTOS.getUser().setRoles(null);
        paymentDTOS.getOrder().setUser(null);
        paymentDTOS.getOrder().getOrderItems().forEach(item->{
            item.getMenu().setReviews(null);
        });

        return Response.<PaymentDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("payment retreived succeessfully by id")
                .data(paymentDTOS)
                .build();

    }
}
