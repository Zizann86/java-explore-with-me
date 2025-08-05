package ru.practicum.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.dal.CategoryRepository;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public void deleteCategory(Long catId) {
        /*Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id %d не найдена", catId)));*/
        Category category = validateCategoryExists(catId);

        log.info("Категория с id {} удалена", catId);
        categoryRepository.delete(category);
    }

    @Override
    public CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto) {
        Category category = validateCategoryExists(catId);
        category.setName(newCategoryDto.getName());
        log.info("Категория обновлена - из: {} в: {}", category, newCategoryDto);
        return CategoryMapper.fromToCategoryDto(categoryRepository.save(category));
    }

    @Override
    public List<CategoryDto> getAllCategories(int from, int size) {
        PageRequest page = PageRequest.of(from > 0 ? from / size : 0, size);
        log.info("Категории успешно найдены");
        return categoryRepository.findAll(page).stream()
                .map(CategoryMapper::fromToCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        Category category = validateCategoryExists(catId);
        log.info("Категория с id {} успешно найдена: {}", category.getId(), category);
        return CategoryMapper.fromToCategoryDto(category);
    }

    private Category validateCategoryExists(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Категория с id %d не найдена", id)));
    }

    /*private Category validateCategoryExists(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("категория с id %d не найдена".formatted(catId));
        } else return categoryRepository.findById(catId).orElseThrow();
    }*/
}
