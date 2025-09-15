package com.app.FoodApp.order.dtos;

import com.app.FoodApp.authUsers.dtos.UserDTO;
import com.app.FoodApp.authUsers.entities.User;
import com.app.FoodApp.enums.OrderStatus;
import com.app.FoodApp.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDTO {
    private Long id;

    private LocalDateTime orderDate;

    private BigDecimal totalAmount;

    private OrderStatus orderStatus;

    private PaymentStatus paymentStatus;

    private UserDTO user; // Customer who is making the order

    private List<OrderItemDTO> orderItems;
}
