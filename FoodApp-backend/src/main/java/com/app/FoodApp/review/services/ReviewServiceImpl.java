package com.app.FoodApp.review.services;

import com.app.FoodApp.authUsers.entities.User;
import com.app.FoodApp.authUsers.services.UserService;
import com.app.FoodApp.enums.OrderStatus;
import com.app.FoodApp.exceptions.BadRequestException;
import com.app.FoodApp.exceptions.NotFoundException;
import com.app.FoodApp.menu.entities.Menu;
import com.app.FoodApp.menu.repositories.MenuRepository;
import com.app.FoodApp.order.entities.Order;
import com.app.FoodApp.order.repositories.OrderItemRepository;
import com.app.FoodApp.order.repositories.OrderRepository;
import com.app.FoodApp.response.Response;
import com.app.FoodApp.review.dtos.ReviewDTO;
import com.app.FoodApp.review.entities.Review;
import com.app.FoodApp.review.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;

    @Transactional
    @Override
    public Response<ReviewDTO> createReview(ReviewDTO reviewDTO) {
        User user = userService.getCurrentLoggedInUser();

        if (reviewDTO.getOrderId() == null || reviewDTO.getMenuId() == null) {
            throw new BadRequestException("Order ID and Menu Item ID are required");
        }

        Menu menu = menuRepository.findById(reviewDTO.getMenuId())
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        Order order = orderRepository.findById(reviewDTO.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found"));

        // Make sure the order belongs to user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Order does not belong to user");
        }

        // Validate order status is DELIVERED
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("You can only review items that have been delivered to you");
        }

        // Validate that menu item was part of this order
        boolean itemInOrder = orderItemRepository.existByOrderIdAndMenuId(
                reviewDTO.getOrderId(),
                reviewDTO.getMenuId());

        if (!itemInOrder) {
            throw new BadRequestException("This menu item was not part of the specified order");
        }

        // Check if user already wrote a review for the item
        if (reviewRepository.existsByUserIdAndMenuIdAndOrderId(
                user.getId(),
                reviewDTO.getMenuId(),
                reviewDTO.getOrderId())) {
            throw new BadRequestException("You've already reviewed this item from this order");
        }

        // Build the DTO
        Review review = Review.builder()
                .user(user)
                .menu(menu)
                .orderId(reviewDTO.getOrderId())
                .rating(reviewDTO.getRating())
                .comment(reviewDTO.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);

        ReviewDTO savedReviewDTO = modelMapper.map(savedReview, ReviewDTO.class);
        savedReviewDTO.setUserName(user.getName());
        savedReviewDTO.setMenuName(menu.getName());

        return Response.<ReviewDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Review created successfully")
                .data(savedReviewDTO)
                .build();
    }

    @Override
    public Response<List<ReviewDTO>> getReviewsForMenu(Long menuId) {
        List<Review> reviews = reviewRepository.findByMenuIdOrderByIdDesc(menuId);

        List<ReviewDTO> reviewDTOS = reviews.stream()
                .map(review -> modelMapper.map(review, ReviewDTO.class))
                .toList();

        return Response.<List<ReviewDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Reviews retrieved successfully")
                .data(reviewDTOS)
                .build();
    }

    @Override
    public Response<Double> getAverageRating(Long menuId) {
        Double averageRating = reviewRepository.calculateAverageRatingByMenuId(menuId);

        return Response.<Double>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Average rating retrieved successfully")
                .data(averageRating != null ? averageRating : 0.0)
                .build();
    }
}
