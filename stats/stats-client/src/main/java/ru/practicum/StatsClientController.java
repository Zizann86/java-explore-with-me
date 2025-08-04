package ru.practicum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StatsClientController {
    private final StatsClient statsClient;

    @PostMapping("/hit")
    public HitDto saveHit(@RequestBody @Valid HitDto hitDto) {
        return statsClient.saveHit(hitDto);
    }

    @GetMapping("/stats")
    public List<StatsDto> getStats(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @NotNull LocalDateTime start,
                                           @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @NotNull LocalDateTime end,
                                           @RequestParam(required = false) List<String> uris,
                                           @RequestParam(defaultValue = "false") Boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalStateException("Неверный диапазон дат");
        }
        String startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return statsClient.getStats(startStr, endStr, uris, unique);
    }
}
