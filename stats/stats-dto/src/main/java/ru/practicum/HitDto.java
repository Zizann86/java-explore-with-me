package ru.practicum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HitDto {
    private Long id;
    @NotNull
    @NotBlank
    private String app;
    @NotNull
    @NotBlank
    private String uri;
    private String ip;
    private LocalDateTime timeStamp;
}
