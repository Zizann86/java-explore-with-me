package ru.practicum.mapper;

import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.model.Category;

public class CategoryMapper {

    public static Category toFromCategoryDto(NewCategoryDto newCategoryDto) {
        return new Category(null, newCategoryDto.getName());
    }

    public static CategoryDto fromToCategoryDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }
}
