package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.HitDto;
import ru.practicum.StatsDto;
import ru.practicum.dal.StatsRepository;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.HitMapper;
import ru.practicum.model.Hit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

import static ru.practicum.mapper.HitMapper.toHitDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    public HitDto create(HitDto hitDto) {
        log.info("Creating hit: {}", hitDto);
        Hit hit = HitMapper.toHit(hitDto);
        Hit savedHit = statsRepository.save(hit);
        log.info("Created hit: {}", savedHit);
        return toHitDto(savedHit);
    }

    @Override
    public Collection<StatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        if (start == null || end == null) {
            throw new ValidationException("Дата не может быть пустой");
        }
        LocalDateTime startTime = parseDate(start);
        LocalDateTime endTime = parseDate(end);

        if (startTime.isAfter(endTime)) {
            throw new ValidationException("Дата начала должна быть раньше даты окончания");
        }
        if (unique) {
            return statsRepository.findUniqueStatsByUrisAndTimestampBetween(startTime, endTime, uris);
        } else {
            return statsRepository.findStatsByUrisAndTimestampBetween(startTime, endTime, uris);
        }
    }

    private LocalDateTime parseDate(String date) {
        if (date == null || date.isBlank()) {
            throw new ValidationException("Дата не может быть пустой");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss");
        return LocalDateTime.parse(date, formatter);
    }
}
