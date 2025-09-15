package com.app.FoodApp.menu.services;

import com.app.FoodApp.aws.AwsS3Service;
import com.app.FoodApp.category.entities.Category;
import com.app.FoodApp.category.repositories.CategoryRepository;
import com.app.FoodApp.exceptions.BadRequestException;
import com.app.FoodApp.exceptions.NotFoundException;
import com.app.FoodApp.menu.dtos.MenuDTO;
import com.app.FoodApp.menu.entities.Menu;
import com.app.FoodApp.menu.repositories.MenuRepository;
import com.app.FoodApp.response.Response;
import com.app.FoodApp.review.dtos.ReviewDTO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
// Service implementation for handling menu-related operations
public class MenuServiceImpl implements MenuService {

    // Repositories for database access
    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;

    // Utility to map between entities and DTOs
    private final ModelMapper modelMapper;

    // Service to interact with AWS S3 (for image upload and deletion)
    private final AwsS3Service awsS3Service;

    /**
     * Creates a new menu item, uploads its image to S3, and saves it to the database.
     */
    @Override
    public Response<MenuDTO> createMenu(MenuDTO menuDTO) {
        // Ensure the category exists
        Category category = categoryRepository.findById(menuDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        MultipartFile imageFile = menuDTO.getImageFile();

        // Validate image presence
        if (imageFile == null || imageFile.isEmpty()) {
            throw new BadRequestException("Menu image is required");
        }

        // Generate a unique image name to avoid conflicts
        // Generate a unique image name to avoid conflicts, replace spaces with "_"
        String originalName = imageFile.getOriginalFilename();
        String safeName = originalName != null ? originalName.replaceAll("\\s+", "_") : "image";
        String imageName = UUID.randomUUID() + "_" + safeName;


        // Upload image to S3 under the "menus/" directory
        URL s3Url = awsS3Service.uploadFile("menus/" + imageName, imageFile);

        // Build Menu entity from DTO
        Menu menu = Menu.builder()
                .name(menuDTO.getName())
                .description(menuDTO.getDescription())
                .price(menuDTO.getPrice())
                .imageUrl(s3Url.toString())
                .category(category)
                .build();

        // Save to DB
        Menu savedMenu = menuRepository.save(menu);

        // Return response with mapped DTO
        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu created successfully")
                .data(modelMapper.map(savedMenu, MenuDTO.class))
                .build();
    }

    /**
     * Updates an existing menu item, replacing its image in S3 if provided.
     */
    @Override
    public Response<MenuDTO> updateMenu(MenuDTO menuDTO) {
        // Ensure menu exists
        Menu existingMenu = menuRepository.findById(menuDTO.getId())
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        // Ensure category exists
        Category category = categoryRepository.findById(menuDTO.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        String imageUrl = existingMenu.getImageUrl();
        MultipartFile imageFile = menuDTO.getImageFile();

        // If new image is uploaded → delete old one from S3 and upload the new one
        if (imageFile != null && !imageFile.isEmpty()) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Extract file name from URL and delete from S3
                String keyName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                awsS3Service.deleteFile("menus/" + keyName);
            }

            // Upload new image with unique name
            String originalName = imageFile.getOriginalFilename();
            String safeName = originalName != null ? originalName.replaceAll("\\s+", "_") : "image";
            String imageName = UUID.randomUUID() + "_" + safeName;
            URL newImageUrl = awsS3Service.uploadFile("menus/" + imageName, imageFile);

            imageUrl = newImageUrl.toString();
        }

        // Update fields if provided
        if (menuDTO.getName() != null && !menuDTO.getName().isBlank())
            existingMenu.setName(menuDTO.getName());
        if (menuDTO.getDescription() != null && !menuDTO.getDescription().isBlank())
            existingMenu.setDescription(menuDTO.getDescription());
        if (menuDTO.getPrice() != null)
            existingMenu.setPrice(menuDTO.getPrice());

        // Set updated values
        existingMenu.setImageUrl(imageUrl);
        existingMenu.setCategory(category);

        // Save updated menu
        Menu updatedMenu = menuRepository.save(existingMenu);

        // Return response with mapped DTO
        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu updated successfully")
                .data(modelMapper.map(updatedMenu, MenuDTO.class))
                .build();
    }

    /**
     * Retrieves a menu by ID and sorts its reviews in descending order.
     */
    @Override
    public Response<MenuDTO> getMenuById(Long id) {
        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        MenuDTO menuDTO = modelMapper.map(existingMenu, MenuDTO.class);

        // Sort reviews newest first
        if (menuDTO.getReviews() != null) {
            menuDTO.getReviews().sort(Comparator.comparing(ReviewDTO::getId).reversed());
        }

        return Response.<MenuDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu retrieved successfully")
                .data(menuDTO)
                .build();
    }

    /**
     * Retrieves all menus, filtered by category and/or search keyword if provided.
     */
    @Override
    public Response<List<MenuDTO>> getAllMenus(Long categoryId, String search) {
        // Build dynamic query specification
        Specification<Menu> specification = buildSpecification(categoryId, search);
        Sort sort = Sort.by(Sort.Direction.DESC, "id"); // Sort newest first

        // Fetch from DB
        List<Menu> menuList = menuRepository.findAll(specification, sort);

        // Convert to DTOs
        List<MenuDTO> menuDTOS = menuList.stream()
                .map(menu -> modelMapper.map(menu, MenuDTO.class))
                .toList();

        return Response.<List<MenuDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menus retrieved successfully")
                .data(menuDTOS)
                .build();
    }

    /**
     * Deletes a menu by ID and removes its image from S3 if it exists.
     */
    @Override
    public Response<?> deleteMenu(Long id) {
        // Ensure menu exists
        Menu menuToDelete = menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found"));

        // Delete associated image from S3
        String imageUrl = menuToDelete.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String keyName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            awsS3Service.deleteFile("menus/" + keyName);
        }

        // Delete menu from DB
        menuRepository.deleteById(id);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menu deleted successfully")
                .build();
    }

    /**
     * Builds a dynamic query specification for filtering menus by category and/or search keyword.
     */
    private Specification<Menu> buildSpecification(Long categoryId, String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by category if provided
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            // Filter by search keyword in name/description
            if (search != null && !search.isBlank()) {
                String searchTerm = "%" + search.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchTerm)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

