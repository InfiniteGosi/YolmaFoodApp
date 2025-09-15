package com.app.FoodApp.cart.repositories;

import com.app.FoodApp.cart.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
