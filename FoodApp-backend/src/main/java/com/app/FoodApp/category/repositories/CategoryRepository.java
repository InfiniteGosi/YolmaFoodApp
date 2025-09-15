package com.app.FoodApp.category.repositories;

import com.app.FoodApp.category.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}
