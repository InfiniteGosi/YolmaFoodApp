package com.app.FoodApp.order.repositories;

import com.app.FoodApp.order.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
            "FROM OrderItem oi " +
            "WHERE oi.order.id = :orderId AND oi.menu.id = :menuId")
    boolean existByOrderIdAndMenuId(
            @Param("orderId") Long orderId,
            @Param("menuId") Long menuId);
}
