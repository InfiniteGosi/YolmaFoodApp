package com.app.FoodApp.order.services;

import com.app.FoodApp.authUsers.entities.User;
import com.app.FoodApp.authUsers.services.UserService;
import com.app.FoodApp.cart.entities.Cart;
import com.app.FoodApp.cart.entities.CartItem;
import com.app.FoodApp.cart.repositories.CartRepository;
import com.app.FoodApp.cart.services.CartService;
import com.app.FoodApp.emailNofitication.dtos.NotificationDTO;
import com.app.FoodApp.emailNofitication.services.NotificationService;
import com.app.FoodApp.enums.OrderStatus;
import com.app.FoodApp.enums.PaymentStatus;
import com.app.FoodApp.exceptions.BadRequestException;
import com.app.FoodApp.exceptions.NotFoundException;
import com.app.FoodApp.menu.dtos.MenuDTO;
import com.app.FoodApp.order.dtos.OrderDTO;
import com.app.FoodApp.order.dtos.OrderItemDTO;
import com.app.FoodApp.order.entities.Order;
import com.app.FoodApp.order.entities.OrderItem;
import com.app.FoodApp.order.repositories.OrderItemRepository;
import com.app.FoodApp.order.repositories.OrderRepository;
import com.app.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
// Service implementation for handling order-related business logic
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;
    private final TemplateEngine templateEngine;
    private final CartService cartService;
    private final CartRepository cartRepository;

    // Base link used for generating payment URLs
    @Value("${base.payment.link}")
    private String basePaymentLink;

    /**
     * Place an order from the currently logged-in user's shopping cart.
     * Steps:
     * 1. Validate user and delivery address.
     * 2. Fetch the user's cart and check that it has items.
     * 3. Convert cart items into order items.
     * 4. Create and save a new order.
     * 5. Save order items and link them to the order.
     * 6. Clear the cart.
     * 7. Send an order confirmation email with a payment link.
     */
    @Transactional
    @Override
    public Response<?> placeOrderFromCart() {
        log.info("Inside place order");

        // Get the logged-in user
        User user = userService.getCurrentLoggedInUser();
        String deliveryAddress = user.getAddress();

        // Ensure delivery address exists
        if (deliveryAddress == null) {
            throw new NotFoundException("Delivery address not found for the user");
        }

        // Retrieve the user's cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Cart not found for the user"));

        List<CartItem> cartItems = cart.getCartItems();

        // Check that the cart has items
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Build order items and calculate total amount
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .menu(cartItem.getMenu())
                    .quantity(cartItem.getQuantity())
                    .pricePerUnit(cartItem.getPricePerUnit())
                    .subtotal(cartItem.getSubtotal())
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getSubtotal());
        }

        // Create the order
        Order order = Order.builder()
                .user(user)
                .orderItems(orderItems)
                .orderDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .orderStatus(OrderStatus.INITIALIZED)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        // Save the order and link order items to it
        Order savedOrder = orderRepository.save(order);
        orderItems.forEach(orderItem -> orderItem.setOrder(savedOrder));
        orderItemRepository.saveAll(orderItems);

        // Clear cart after placing the order
        cartService.clearShoppingCart();

        // Convert entity to DTO
        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);

        // Send confirmation email
        sendOrderConfirmationEmail(user, orderDTO);

        // Return API response
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Your order has been received!\n We've sent a secure payment link to your email.\n Please proceed for payment to confirm you order.")
                .build();
    }

    /**
     * Fetch an order by its ID.
     */
    @Override
    public Response<OrderDTO> getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
        return Response.<OrderDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(orderDTO)
                .build();
    }

    /**
     * Retrieve all orders with optional filtering by status.
     * Results are paginated and sorted by newest first.
     */
    @Override
    public Response<Page<OrderDTO>> getAllOrders(OrderStatus orderStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Order> orderPage;

        // Filter by status if provided
        if (orderStatus != null) {
            orderPage = orderRepository.findByOrderStatus(orderStatus, pageable);
        } else {
            orderPage = orderRepository.findAll(pageable);
        }

        // Convert to DTOs and strip unnecessary fields
        Page<OrderDTO> orderDTOPage = orderPage.map(order -> {
            OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
            orderDTO.getOrderItems().forEach(orderItemDTO -> orderItemDTO.getMenu().setReviews(null));
            return orderDTO;
        });

        return Response.<Page<OrderDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(orderDTOPage)
                .build();
    }

    /**
     * Retrieve all orders for the current user.
     */
    @Override
    public Response<List<OrderDTO>> getOrdersOfUser() {
        User user = userService.getCurrentLoggedInUser();

        // Fetch user's orders (latest first)
        List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(user);

        // Convert to DTOs
        List<OrderDTO> orderDTOS = orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class)).toList();

        // Remove user and reviews from DTO for response clarity
        orderDTOS.forEach(orderDTO -> {
            orderDTO.setUser(null);
            orderDTO.getOrderItems().forEach(item -> item.getMenu().setReviews(null));
        });

        return Response.<List<OrderDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Orders for user retrieved successfully")
                .data(orderDTOS)
                .build();
    }

    /**
     * Retrieve a specific order item by ID.
     */
    @Override
    public Response<OrderItemDTO> getOrderItemById(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new NotFoundException("Order item not found"));

        // Convert to DTO
        OrderItemDTO orderItemDTO = modelMapper.map(orderItem, OrderItemDTO.class);
        orderItemDTO.setMenu(modelMapper.map(orderItem.getMenu(), MenuDTO.class));

        return Response.<OrderItemDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order item retrieved successfully")
                .data(orderItemDTO)
                .build();
    }

    /**
     * Update the status of an existing order.
     */
    @Override
    public Response<OrderDTO> updateOrderStatus(OrderDTO orderDTO) {
        Order order = orderRepository.findById(orderDTO.getId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        order.setOrderStatus(orderDTO.getOrderStatus());
        orderRepository.save(order);

        return Response.<OrderDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Order status updated successfully")
                .build();
    }

    /**
     * Count how many unique customers have placed orders.
     */
    @Override
    public Response<Long> countUniqueCustomers() {
        long uniqueCustomerCount = orderRepository.countDistinctUsers();
        return Response.<Long>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Unique customer count retrieved successfully")
                .data(uniqueCustomerCount)
                .build();
    }

    /**
     * Send order confirmation email to the customer with order details and payment link.
     */
    private void sendOrderConfirmationEmail(User user, OrderDTO orderDTO) {
        String subject = "Your Order Confirmation - Order #" + orderDTO.getId();

        // Setup Thymeleaf context for email template
        Context context = new Context(Locale.getDefault());
        context.setVariable("customerName", user.getName());
        context.setVariable("orderId", String.valueOf(orderDTO.getId()));
        context.setVariable("orderDate", String.valueOf(orderDTO.getOrderDate()));
        context.setVariable("totalAmount", orderDTO.getTotalAmount().toString());
        context.setVariable("deliveryAddress", orderDTO.getUser().getAddress());
        context.setVariable("currentYear", Year.now().getValue());

        // Build HTML for order items
        StringBuilder orderItemsHtml = new StringBuilder();
        for (OrderItemDTO item : orderDTO.getOrderItems()) {
            orderItemsHtml.append("<div class=\"order-item\">")
                    .append("<p>").append(item.getMenu().getName()).append(" x ").append(item.getQuantity()).append("</p>")
                    .append("<p> $ ").append(item.getSubtotal()).append("</p>")
                    .append("</div>");
        }
        context.setVariable("orderItemsHtml", orderItemsHtml.toString());
        context.setVariable("totalItems", orderDTO.getOrderItems().size());

        // Payment link for completing the order
        String paymentLink = basePaymentLink + orderDTO.getId() + "&amount=" + orderDTO.getTotalAmount();
        context.setVariable("paymentLink", paymentLink);

        // Render Thymeleaf template to HTML string
        String emailBody = templateEngine.process("order-confirmation", context);

        // Send the email
        notificationService.sendEmail(NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject(subject)
                .body(emailBody)
                .isHtml(true)
                .build());
    }
}

