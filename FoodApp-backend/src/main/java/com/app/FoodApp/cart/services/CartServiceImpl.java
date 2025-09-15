package com.app.FoodApp.cart.services;

import com.app.FoodApp.authUsers.entities.User;
import com.app.FoodApp.authUsers.services.UserService;
import com.app.FoodApp.cart.dtos.CartDTO;
import com.app.FoodApp.cart.entities.Cart;
import com.app.FoodApp.cart.entities.CartItem;
import com.app.FoodApp.cart.repositories.CartItemRepository;
import com.app.FoodApp.cart.repositories.CartRepository;
import com.app.FoodApp.exceptions.NotFoundException;
import com.app.FoodApp.menu.entities.Menu;
import com.app.FoodApp.menu.repositories.MenuRepository;
import com.app.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional          // Ensures all methods run inside a transaction
public class CartServiceImpl implements CartService {

    // Repositories and services injected via constructor
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final MenuRepository menuRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    /**
     * Adds an item to the user's cart.
     * If the cart doesn't exist, creates a new one.
     * If the item already exists, increments its quantity instead of creating a duplicate.
     */
    @Override
    public Response<?> addItemToCart(CartDTO cartDTO) {
        Long menuId = cartDTO.getMenuId();
        int quantity = cartDTO.getQuantity();

        // Get currently logged-in user
        User user = userService.getCurrentLoggedInUser();

        // Check if menu item exists
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        // Get user's cart, or create one if it doesn’t exist
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setCartItems(new ArrayList<>());
                    return cartRepository.save(newCart);
                });

        // Look for the menu item in the cart
        Optional<CartItem> optionalCartItem = cart.getCartItems().stream()
                .filter(cartItem -> cartItem.getMenu().getId().equals(menuId))
                .findFirst();

        if (optionalCartItem.isPresent()) {
            // Item already in cart → increase quantity and update subtotal
            CartItem cartItem = optionalCartItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setSubtotal(cartItem.getPricePerUnit().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            cartItemRepository.save(cartItem);
        } else {
            // Item not in cart → create a new CartItem
            CartItem newCartItem = CartItem.builder()
                    .cart(cart)
                    .menu(menu)
                    .quantity(quantity)
                    .pricePerUnit(menu.getPrice())
                    .subtotal(menu.getPrice().multiply(BigDecimal.valueOf(quantity)))
                    .build();
            cart.getCartItems().add(newCartItem);
            cartItemRepository.save(newCartItem);
        }

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Successfully added cart item to cart")
                .build();
    }

    /**
     * Increments the quantity of an item in the cart by 1.
     */
    @Override
    public Response<?> incrementItem(Long menuId) {
        User user = userService.getCurrentLoggedInUser();

        // Find user's cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        // Find specific cart item by menuId
        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getMenu().getId().equals(menuId))
                .findFirst().orElseThrow(() -> new NotFoundException("Cart item not found"));

        // Update quantity and subtotal
        int newQuantity = cartItem.getQuantity() + 1;
        cartItem.setQuantity(newQuantity);
        cartItem.setSubtotal(cartItem.getPricePerUnit().multiply(BigDecimal.valueOf(newQuantity)));

        cartItemRepository.save(cartItem);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Item quantity incremented successfully")
                .build();
    }

    /**
     * Decrements the quantity of an item in the cart by 1.
     * If quantity reaches 0, removes the item from the cart.
     */
    @Override
    public Response<?> decrementItem(Long menuId) {
        User user = userService.getCurrentLoggedInUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getMenu().getId().equals(menuId))
                .findFirst().orElseThrow(() -> new NotFoundException("Cart item not found"));

        int newQuantity = cartItem.getQuantity() - 1;

        if (newQuantity > 0) {
            // Update quantity and subtotal
            cartItem.setQuantity(newQuantity);
            cartItem.setSubtotal(cartItem.getPricePerUnit().multiply(BigDecimal.valueOf(newQuantity)));
            cartItemRepository.save(cartItem);
        } else {
            // If quantity is zero, remove the item entirely
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        }

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Item quantity decremented successfully")
                .build();
    }

    /**
     * Removes a specific item from the cart.
     */
    @Override
    public Response<?> removeItem(Long cartItemId) {
        User user = userService.getCurrentLoggedInUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        // Ensure the cartItem belongs to this user
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new NotFoundException("Cart item not found"));

        if (!cart.getCartItems().contains(cartItem)) {
            throw new NotFoundException("Cart item does not belong to this user's cart");
        }

        // Remove item from cart
        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Item removed from cart successfully")
                .build();
    }

    /**
     * Retrieves the user's shopping cart along with total amount.
     */
    @Override
    @Transactional(readOnly = true)
    public Response<CartDTO> getShoppingCart() {
        User user = userService.getCurrentLoggedInUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Cart not found for user"));

        List<CartItem> cartItems = cart.getCartItems();

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (cartItems != null) { // Add null check here
            for (CartItem item : cartItems) {
                totalAmount = totalAmount.add(item.getSubtotal());
            }
        }

        cartDTO.setTotalAmount(totalAmount); //set the totalAmount

        //remove the review from the response
        if (cartDTO.getCartItems() != null) {
            cartDTO.getCartItems()
                    .forEach(item -> item.getMenu().setReviews(null));
        }

        return Response.<CartDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Shopping cart retrieved successfully")
                .data(cartDTO)
                .build();

    }

    /**
     * Clears all items from the user's cart.
     */
    @Override
    public Response<?> clearShoppingCart() {
        User user = userService.getCurrentLoggedInUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Cart not found for user"));

        // Remove all cart items
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();

        cartRepository.save(cart);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Shopping cart cleared successfully")
                .build();
    }
}
