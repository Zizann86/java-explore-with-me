package ru.practicum.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationExceptions(Exception ex) {
        List<String> errors;
        String reason;

        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException manve = (MethodArgumentNotValidException) ex;
            errors = manve.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
            reason = "Неверные данные запроса";
        } else {
            ConstraintViolationException cve = (ConstraintViolationException) ex;
            errors = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.toList());
            reason = "Недопустимые параметры запроса";
        }

        return ApiError.builder()
                .errors(errors)
                .message("Не удалось выполнить проверку")
                .reason(reason)
                .status(HttpStatus.BAD_REQUEST.name())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException ex) {
        return ApiError.builder()
                .errors(List.of(ex.getMessage()))
                .message(ex.getMessage())
                .reason("Ресурс не найден")
                .status(HttpStatus.NOT_FOUND.name())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleValidationException(ValidationException ex) {
        return ApiError.builder()
                .errors(List.of(ex.getMessage()))
                .message("Ошибка валидации данных")
                .reason("Нарушение правил бизнес-логики")
                .status(HttpStatus.CONFLICT.name())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
