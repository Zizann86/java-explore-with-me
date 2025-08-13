package ru.practicum.service.category;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.dal.CategoryRepository;
import ru.practicum.dal.EventRepository;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventsRepository;

    @Override
    public CategoryDto save(NewCategoryDto newCategoryDto) {
        String newName = newCategoryDto.getName();
        if (categoryRepository.existsByNameIgnoreCase(newName)) {
            throw new ConflictException(("Категория " + newName + " уже существует"));
        }
        Category category = categoryRepository.save(CategoryMapper.toFromCategoryDto(newCategoryDto));
        log.info("Категория с id {} успешно создана", category.getId());
        return CategoryMapper.fromToCategoryDto(category);
    }

    @Override
    public void deleteCategory(Long catId) {
        Category category = validateCategoryExists(catId);
        List<Event> events = eventsRepository.findByCategory(category);
        if (!events.isEmpty()) {
            throw new ConflictException("Не удается удалить категорию из-за использования некоторых событий");
        }
        log.info("Категория с id {} удалена", catId);
        categoryRepository.delete(category);
    }

    @Override
    public CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto) {
        Category category = validateCategoryExists(catId);
        String newName = newCategoryDto.getName();
        String currentName = category.getName();

        if (newName.equals(currentName)) {
            return CategoryMapper.fromToCategoryDto(category);
        }

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(newName, catId)) {
            throw new ConflictException("Категория " + newName + " уже существует");
        }

        category.setName(newName);
        log.info("Категория обновлена - из: {} в: {}", currentName, newName);
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

}
