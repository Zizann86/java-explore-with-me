package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.HitDto;
import ru.practicum.StatsDto;
import ru.practicum.service.StatsService;

import java.util.Collection;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@RestController
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    public HitDto createHit(@RequestBody @Valid HitDto hitDto) {
        log.info("Создание запроса {} в сервисе", hitDto);
        return statsService.create(hitDto);
    }

    @GetMapping("/stats")
    public Collection<StatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @NotNull String start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @NotNull String end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam(defaultValue = "false") Boolean unique
    ) {
        log.info("Получение статистики по параметрам: start={}; end={}; uris={}; unique={}", start, end, uris, unique);
        return statsService.getStats(start, end, uris, unique);
    }
}
