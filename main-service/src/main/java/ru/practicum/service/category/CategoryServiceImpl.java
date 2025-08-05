package ru.practicum.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dal.CategoryRepository;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public CategoryDto save(NewCategoryDto newCategoryDto) {
        Category category = categoryRepository.save(CategoryMapper.toFromCategoryDto(newCategoryDto));
        log.info("Категория с id {} успешно создана", category.getId());
        return CategoryMapper.fromToCategoryDto(category);
    }

}
