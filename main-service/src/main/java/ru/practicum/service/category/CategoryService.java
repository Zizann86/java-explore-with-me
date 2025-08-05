package ru.practicum.service.category;

import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

public interface CategoryService {

    CategoryDto save(NewCategoryDto newCategoryDto);
}
