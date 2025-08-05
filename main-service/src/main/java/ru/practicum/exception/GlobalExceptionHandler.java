package ru.practicum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException ex) {
        return ApiError.builder()
                .errors(List.of(ex.getMessage()))
                .message(ex.getMessage())
                .reason("Resource not found")
                .status(HttpStatus.NOT_FOUND.name())
                .timestamp(LocalDateTime.now().toString())
                .build();
    }
}
