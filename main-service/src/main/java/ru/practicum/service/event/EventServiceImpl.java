package ru.practicum.service.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.Location;
import ru.practicum.model.User;
import ru.practicum.model.enums.ActionStateAdmin;
import ru.practicum.model.enums.State;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

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
        event.setState(State.PENDING);
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

    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        LocalDateTime validDate = LocalDateTime.now().plusHours(2L);
        if (updateEventAdminRequest.getEventDate() != null && updateEventAdminRequest.getEventDate().isBefore(validDate)) {
            throw new ValidationException("Дата мероприятия должна быть назначена через два часа");
        }
        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("событие не найдено с идентификатором id = " + eventId));
        if (updateEventAdminRequest.getStateAction() != null) {
            if (updateEventAdminRequest.getStateAction() == ActionStateAdmin.REJECT_EVENT &&
                    oldEvent.getState() == State.PUBLISHED) {
                throw new ConflictException("Невозможно отменить опубликованное событие");
            }
            if (updateEventAdminRequest.getStateAction() == ActionStateAdmin.PUBLISH_EVENT &&
                    oldEvent.getState() != State.PENDING) {
                throw new ConflictException("Не удается опубликовать событие, не имеющее статуса ожидающего");
            }
            if (updateEventAdminRequest.getStateAction() == ActionStateAdmin.PUBLISH_EVENT &&
                    oldEvent.getEventDate().minusHours(1L).isBefore(LocalDateTime.now())) {
                throw new ConflictException("Невозможно опубликовать событие менее чем за 1 час до начала");
            }
        }
        boolean hasChanges = false;
        Event eventForUpdate = updateUniversal(oldEvent, updateEventAdminRequest);
        if (eventForUpdate == null) {
            eventForUpdate = oldEvent;
        } else {
            hasChanges = true;
        }

        LocalDateTime gotEventDate = updateEventAdminRequest.getEventDate();
        if (gotEventDate != null) {
            if (gotEventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ConflictException("Некорректные параметры даты.Дата начала " +
                        "изменяемого события должна " + "быть не ранее чем за час от даты публикации.");
            }
            eventForUpdate.setEventDate(updateEventAdminRequest.getEventDate());
            hasChanges = true;
        }
        ActionStateAdmin gotAction = updateEventAdminRequest.getStateAction();
        if (gotAction != null) {
            if (ActionStateAdmin.PUBLISH_EVENT.equals(gotAction)) {
                eventForUpdate.setState(State.PUBLISHED);
                eventForUpdate.setPublishedOn(LocalDateTime.now());
                hasChanges = true;
            } else if (ActionStateAdmin.REJECT_EVENT.equals(gotAction)) {
                eventForUpdate.setState(State.CANCELED);
                hasChanges = true;
            }
        }
        Event eventAfterUpdate = null;
        if (hasChanges) {
            eventAfterUpdate = eventRepository.save(eventForUpdate);
        }
        return eventAfterUpdate != null ? EventMapper.toEventFullDto(eventAfterUpdate) : null;
    }

    @Override
    public List<EventFullDto> findEventsByAdmin(List<Long> users,
                                             List<String> states,
                                             List<Long> categories,
                                             String rangeStart,
                                             String rangeEnd,
                                             int from,
                                             int size) {
        Pageable pageable = PageRequest.of(from, size);

        LocalDateTime start = parseDateTime(rangeStart, null);
        LocalDateTime end = parseDateTime(rangeEnd, null);

        if (rangeStart != null) {
            start = LocalDateTime.parse(rangeStart, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        if (rangeEnd != null) {
            end = LocalDateTime.parse(rangeEnd, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return eventRepository.findAllByFilter(users, states, categories, start, end, pageable).stream()
                .map(EventMapper::toEventFullDto)
                .toList();
    }

    @Override
    public List<EventShortDto> findEventsByPublic(String text,
                                                  List<Long> categories,
                                                  Boolean paid,
                                                  String rangeStart,
                                                  String rangeEnd,
                                                  Boolean onlyAvailable,
                                                  String sort,
                                                  int from,
                                                  int size,
                                                  HttpServletRequest request) {

        Pageable pageable;
        if (sort != null && !sort.isBlank()) {
            String sortField = mapSortParameterToFieldName(sort);
            pageable = PageRequest.of(from, size, Sort.by(sortField).descending());
        } else {
            pageable = PageRequest.of(from, size);
        }

        LocalDateTime start = parseDateTime(rangeStart, LocalDateTime.now());
        LocalDateTime end = parseDateTime(rangeEnd, null);

        sendStat(request);

        List<Event> events = eventRepository.findAllByPublic(text, categories, paid, start, end, onlyAvailable,
                State.PUBLISHED, pageable);
        List<String> uris = events.stream()
                .map(x -> "/event/" + x.getId())
                .toList();

        String startStatsDate = events.stream()
                .map(Event::getPublishedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .map(date -> date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .orElse("1970-01-01 00:00:00");
        String endStatsDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<StatsDto> statViews = statsClient.getStats(startStatsDate, endStatsDate, uris, false);

        statViews.forEach(stats -> {
            String uri = stats.getUri();
            try {
                if (uri.matches(".*\\d+$")) {
                    String lastPart = uri.substring(uri.lastIndexOf('/') + 1);
                    Long eventId = Long.parseLong(lastPart);
                    events.stream()
                            .filter(e -> e.getId().equals(eventId))
                            .findFirst()
                            .ifPresent(e -> e.setViews(stats.getHits()));
                }
            } catch (Exception e) {
                log.warn("Не удалось обработать URI: {}", uri);
            }
        });

        eventRepository.saveAll(events);
        return events.stream()
                .map(EventMapper::toEventShortDto)
                .toList();
    }

    private LocalDateTime parseDateTime(String dateTimeStr, LocalDateTime defaultIfNull) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return defaultIfNull;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            throw new ValidationException("Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss");
        }
    }

    private String mapSortParameterToFieldName(String sort) {
        return switch (sort.toUpperCase()) {
            case "EVENT_DATE" -> "eventDate";  // Используем имя поля из сущности
            case "VIEWS" -> "views";
            default -> throw new IllegalArgumentException(
                    "Недопустимый параметр сортировки. Используйте: EVENT_DATE or VIEWS"
            );
        };
    }


    private void sendStat(HttpServletRequest request) {
        statsClient.saveHit(HitDto.builder()
                .app("main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    private Event updateUniversal(Event oldEvent, UpdateEventAdminRequest updateEvent) {
        boolean hasChanges = false;
        String gotAnnotation = updateEvent.getAnnotation();
        if (gotAnnotation != null && !gotAnnotation.isBlank()) {
            oldEvent.setAnnotation(gotAnnotation);
            hasChanges = true;
        }
        Long gotCategory = updateEvent.getCategory();
        if (gotCategory != null) {
            Category category = categoryRepository.findById(gotCategory).orElseThrow(
                    () -> new NotFoundException("Категории с id = " + gotCategory + " не существует"));
            oldEvent.setCategory(category);
            hasChanges = true;
        }
        String gotDescription = updateEvent.getDescription();
        if (gotDescription != null && !gotDescription.isBlank()) {
            oldEvent.setDescription(gotDescription);
            hasChanges = true;
        }
        if (updateEvent.getLocation() != null) {
            Location location = LocationMapper.toFromLocationDto(updateEvent.getLocation());
            oldEvent.setLocation(location);
            hasChanges = true;
        }
        Integer gotParticipantLimit = updateEvent.getParticipantLimit();
        if (gotParticipantLimit != null) {
            oldEvent.setParticipantLimit(gotParticipantLimit);
            hasChanges = true;
        }
        if (updateEvent.getPaid() != null) {
            oldEvent.setPaid(updateEvent.getPaid());
            hasChanges = true;
        }
        Boolean requestModeration = updateEvent.getRequestModeration();
        if (requestModeration != null) {
            oldEvent.setRequestModeration(requestModeration);
            hasChanges = true;
        }
        String gotTitle = updateEvent.getTitle();
        if (gotTitle != null && !gotTitle.isBlank()) {
            oldEvent.setTitle(gotTitle);
            hasChanges = true;
        }
        if (!hasChanges) {

            oldEvent = null;
        }
        return oldEvent;
    }
}
