package com.app.FoodApp.role.services;

import com.app.FoodApp.response.Response;
import com.app.FoodApp.role.dtos.RoleDTO;

import java.util.List;

public interface RoleService {
    Response<RoleDTO> createRole(RoleDTO roleDTO);
    Response<RoleDTO> updateRole(RoleDTO roleDTO);
    Response<List<RoleDTO>> getAllRoles();
    Response<?> deleteRole(Long id);
}
