package com.app.FoodApp.category.services;

import com.app.FoodApp.category.dtos.CategoryDTO;
import com.app.FoodApp.response.Response;

import java.util.List;

public interface CategoryService {
    Response<CategoryDTO> addCategory(CategoryDTO categoryDTO);
    Response<List<CategoryDTO>> getAllCategories();
    Response<CategoryDTO> getCategoryById(Long id);
    Response<CategoryDTO> updateCategory(CategoryDTO categoryDTO);
    Response<?> deleteCategory(Long id);
}
