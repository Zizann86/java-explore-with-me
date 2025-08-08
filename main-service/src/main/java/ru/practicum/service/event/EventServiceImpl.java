package ru.practicum.service.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.HitDto;
import ru.practicum.StatsClient;
import ru.practicum.StatsDto;
import ru.practicum.dal.CategoryRepository;
import ru.practicum.dal.EventRepository;
import ru.practicum.dal.LocationRepository;
import ru.practicum.dal.UserRepository;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.Location;
import ru.practicum.model.User;
import ru.practicum.model.enums.State;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final StatsClient statsClient;

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final CategoryRepository categoryRepository;

    private final LocationRepository locationRepository;



    @Override
    public EventFullDto save(Long userId, NewEventDto newEventDto) {
        LocalDateTime createdOn = LocalDateTime.now().plusHours(2L);
        if (newEventDto.getEventDate() != null && newEventDto.getEventDate().isBefore(createdOn)) {
            throw new ValidationException("Дата события должна быть назначена через два часа");
        }
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id %d не найден.", userId)));
        Event event = EventMapper.toEvent(newEventDto);
        Category category = categoryRepository.findById(newEventDto.getCategory()).orElseThrow(
                () -> new NotFoundException("Категории с id = " + newEventDto.getCategory() + " не существует"));
        event.setCategory(category);
        event.setInitiator(initiator);
        event.setState(State.PUBLISHED);
        event.setCreatedOn(createdOn);
        if (newEventDto.getLocation() != null) {
            Location location = locationRepository.save(LocationMapper.toFromLocationDto(newEventDto.getLocation()));
            event.setLocation(location);
        }
        Event eventSaved = eventRepository.save(event);

        EventFullDto eventFullDto = EventMapper.toEventFullDto(eventSaved);
        eventFullDto.setViews(0L);
        eventFullDto.setConfirmedRequests(0);
        log.info("Событие с id {} успешно создано", eventFullDto.getId());
        return eventFullDto;
    }

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("События с id = " + eventId + " не существует"));
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new NotFoundException("Событие с id = " + eventId + " не опубликовано");
        }
        sendStat(request);

        LocalDateTime statsStartDate = event.getPublishedOn();
        if (statsStartDate == null) {
            statsStartDate = event.getCreatedOn();
        }
        List<StatsDto> views = statsClient.getStats(
                statsStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                List.of(request.getRequestURI()),
                true);

        if (!views.isEmpty()) {
            event.setViews(views.get(0).getHits());
            eventRepository.save(event);
        }
        log.info("Событие с id {} успешно найдено:", eventId);
        return EventMapper.toEventFullDto(event);
    }

    private void sendStat(HttpServletRequest request) {
        statsClient.saveHit(HitDto.builder()
                .app("main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timeStamp(LocalDateTime.now())
                .build());
    }
}
