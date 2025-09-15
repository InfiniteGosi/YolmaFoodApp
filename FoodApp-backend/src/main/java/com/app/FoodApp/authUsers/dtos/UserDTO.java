package com.app.FoodApp.authUsers.dtos;

import com.app.FoodApp.role.dtos.RoleDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;

    private String profileUrl;

    // Does not show password when returning the user
    // The field can be written to (deserialized) from incoming JSON,
    // but will not be included (serialized) in outgoing JSON.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private Boolean isActive;

    private String address;

    private List<RoleDTO> roles;

    private MultipartFile imageFile;
}
