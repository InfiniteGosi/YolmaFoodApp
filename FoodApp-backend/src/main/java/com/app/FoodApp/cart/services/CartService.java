package com.app.FoodApp.cart.services;

import com.app.FoodApp.cart.dtos.CartDTO;
import com.app.FoodApp.response.Response;

public interface CartService {
    Response<?> addItemToCart(CartDTO cartDTO);
    Response<?> incrementItem(Long menuId);
    Response<?> decrementItem(Long menuId);
    Response<?> removeItem(Long cartItemId);
    Response<CartDTO> getShoppingCart();
    Response<?> clearShoppingCart();

}
