package com.smartcommerce.product.service;

import com.smartcommerce.product.dto.CategoryDtos.CategoryRequest;
import com.smartcommerce.product.entity.Category;
import com.smartcommerce.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> listAll() {
        return categoryRepository.findAll();
    }

    public Category create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("A category with this name already exists");
        }
        Category category = Category.builder()
                .name(request.getName())
                .parentCategoryId(request.getParentCategoryId())
                .build();
        return categoryRepository.save(category);
    }
}
