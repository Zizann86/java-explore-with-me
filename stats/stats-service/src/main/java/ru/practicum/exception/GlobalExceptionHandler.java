package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError validationHandler(final ValidationException e) {
        log.error("Ошибка при валидации данных: {}", e.getMessage());
        return ApiError.builder()
                .errorCode(HttpStatus.BAD_REQUEST.value())
                .description(e.getMessage())
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handler(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return ApiError.builder()
                .errorCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .description("Внутренняя ошибка сервера")
                .build();
    }
}
