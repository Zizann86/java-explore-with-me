package ru.practicum.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NewCategoryDto {
    @NotBlank(message = "Название категории не может быть пустым.")
    @Size(min = 1, max = 50, message = "Название должно быть от 1 до 50 символов.")
    private String name;
}
