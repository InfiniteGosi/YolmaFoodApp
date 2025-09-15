package com.app.FoodApp.menu.services;

import com.app.FoodApp.menu.dtos.MenuDTO;
import com.app.FoodApp.response.Response;

import java.util.List;

public interface MenuService {
    Response<MenuDTO> createMenu(MenuDTO menuDTO);
    Response<MenuDTO> updateMenu(MenuDTO menuDTO);
    Response<MenuDTO> getMenuById(Long id);
    Response<List<MenuDTO>> getAllMenus(Long categoryId, String search);
    Response<?> deleteMenu(Long id);
}
