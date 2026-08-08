package com.smartcommerce.product.controller;

import com.smartcommerce.product.dto.CategoryDtos.CategoryRequest;
import com.smartcommerce.product.dto.CategoryDtos.CategoryResponse;
import com.smartcommerce.product.entity.Category;
import com.smartcommerce.product.security.CurrentUser;
import com.smartcommerce.product.security.CurrentUserResolver;
import com.smartcommerce.product.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listAll() {
        List<CategoryResponse> categories = categoryService.listAll().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request,
                                                     HttpServletRequest httpRequest) {
        CurrentUser user = currentUserResolver.resolve(httpRequest);
        if (!user.isAdmin()) {
            throw new SecurityException("Admin role required to create categories");
        }
        Category created = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .parentCategoryId(c.getParentCategoryId())
                .build();
    }
}
