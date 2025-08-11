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
import ru.practicum.dal.*;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.*;
import ru.practicum.model.enums.ActionState;
import ru.practicum.model.enums.ActionStateAdmin;
import ru.practicum.model.enums.RequestStatus;
import ru.practicum.model.enums.State;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
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

    private final RequestRepository requestRepository;


    @Override
    public EventFullDto save(Long userId, NewEventDto newEventDto) {
        LocalDateTime createdOn = LocalDateTime.now();
        validateEventDate(newEventDto.getEventDate(), LocalDateTime.now());
        User initiator = validateUserExist(userId);
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

    @Transactional
    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest request) {
        Event event = validateEventExist(eventId);
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new NotFoundException("Событие с id = " + eventId + " не опубликовано");
        }
        sendStat(request);
        updateEventViews(event, request);
        return EventMapper.toEventFullDto(event);
    }

    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        LocalDateTime validDate = LocalDateTime.now();
        validateEventDate(updateEventAdminRequest.getEventDate(), validDate);
        Event oldEvent = validateEventExist(eventId);
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
            validateEventDate(gotEventDate, LocalDateTime.now());
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

        Pageable pageable = createPageable(from, size, null);

        LocalDateTime start = parseDateTime(rangeStart, null);
        LocalDateTime end = parseDateTime(rangeEnd, null);

        if (start != null && end != null && end.isBefore(start)) {
            throw new ValidationException("Дата окончания не может быть раньше даты начала");
        }

        List<Event> events = eventRepository.findAllByFilter(
                users,
                states,
                categories,
                start,
                end,
                pageable
        );

        if (events.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Request>> confirmedRequests = requestRepository
                .findAllByEventIdInAndStatus(
                        events.stream().map(Event::getId).collect(Collectors.toList()),
                        RequestStatus.CONFIRMED
                ).stream()
                .collect(Collectors.groupingBy(r -> r.getEvent().getId()
                ));

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        String statsStart = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .map(date -> date.format(Constants.DATE_TIME_FORMATTER))
                .orElse("1970-01-01 00:00:00");

        List<StatsDto> stats = statsClient.getStats(
                statsStart,
                LocalDateTime.now().format(Constants.DATE_TIME_FORMATTER),
                uris,
                false
        );

        Map<Long, Long> views = stats.stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.getUri().substring(stat.getUri().lastIndexOf('/') + 1)),
                        StatsDto::getHits
                ));

        return events.stream()
                .map(event -> {
                    EventFullDto dto = EventMapper.toEventFullDto(event);
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), List.of()).size());
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
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


        Pageable pageable = createPageable(from, size, sort);
        LocalDateTime start = parseDateTime(rangeStart, LocalDateTime.now());
        LocalDateTime end = parseDateTime(rangeEnd, null);
        if (start == null) {
            throw new ValidationException("Дата начала не может быть пустой");
        }

        if (end != null && end.isBefore(start)) {
            throw new ValidationException("Дата окончания не может быть раньше даты начала");
        }
        sendStat(request);
        List<Event> events = eventRepository.findAllByPublic(
                text, categories, paid, start, end, onlyAvailable,
                State.PUBLISHED, pageable
        );
        for (Event event : events) {
            updateEventViews(event, request);
        }
        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            result.add(EventMapper.toEventShortDto(event));
        }
        return result;
    }

    @Override
    public List<EventShortDto> findEvents(Long userId, int from, int size) {
        validateUserExist(userId);

        Pageable pageable = createPageable(from, size, null);

        return eventRepository.findByUserId(userId, pageable).stream()
                .map(EventMapper::toEventShortDto)
                .toList();
    }

    @Override
    public EventFullDto findEvent(Long eventId, Long userId) {
        validateUserExist(userId);
        Event event = validateEventExist(eventId);
        return EventMapper.toEventFullDto(event);
    }

    @Override
    public EventFullDto updateEvent(Long eventId, Long userId, UpdateEventUserRequest updateEventUserRequest) {
        LocalDateTime validDate = LocalDateTime.now();
        validateEventDate(updateEventUserRequest.getEventDate(), validDate);
        validateUserExist(userId);
        Event oldEvent = eventRepository.findByInitiatorIdAndId(userId, eventId).orElseThrow(
                () -> new NotFoundException("События с id = " + eventId + "и с пользователем с id = " + userId +
                        " не существует"));
        if (oldEvent.getState().equals(State.PUBLISHED)) {
            throw new ConflictException("Статус события не может быть обновлен, так как со статусом PUBLISHED");
        }
        if (!oldEvent.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь с id= " + userId + " не автор события");
        }
        Event eventForUpdate = updateUniversal(oldEvent, updateEventUserRequest);
        boolean hasChanges = false;
        if (eventForUpdate == null) {
            eventForUpdate = oldEvent;
        } else {
            hasChanges = true;
        }
        LocalDateTime newDate = updateEventUserRequest.getEventDate();
        if (newDate != null) {
            if (LocalDateTime.now().isBefore(newDate)) {
                throw new ConflictException("Поле должно содержать дату, которая еще не наступила.");
            }
            eventForUpdate.setEventDate(newDate);
            hasChanges = true;
        }
        ActionState stateAction = updateEventUserRequest.getStateAction();
        if (stateAction != null) {
            switch (stateAction) {
                case SEND_TO_REVIEW:
                    eventForUpdate.setState(State.PENDING);
                    hasChanges = true;
                    break;
                case CANCEL_REVIEW:
                    eventForUpdate.setState(State.CANCELED);
                    hasChanges = true;
                    break;
            }
        }
        Event eventAfterUpdate = null;
        if (hasChanges) {
            eventAfterUpdate = eventRepository.save(eventForUpdate);
        }

        return eventAfterUpdate != null ? EventMapper.toEventFullDto(eventAfterUpdate) : null;
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestsStatus(Long userId,
                                                               Long eventId,
                                                               EventRequestStatusUpdateRequest request) {
        User user = validateUserExist(userId);
        Event event = validateEventExist(eventId);
        if (!event.getInitiator().equals(user))
            throw new ValidationException("Пользователь с id = " + userId + " не является инициатором события с id = " + eventId);
        Collection<Request> requests = requestRepository.findAllByEventId_IdAndIdIn(eventId,
                request.getRequestIds());
        int limit = event.getParticipantLimit() - event.getConfirmedRequests().intValue();
        int confirmed = event.getConfirmedRequests().intValue();
        if (limit == 0)
            throw new ConflictException("Достигнут лимит");
        for (Request req : requests) {
            if (!req.getStatus().equals(RequestStatus.PENDING))
                throw new ConflictException("Status of the request with id = " + req.getId() + " is " + req.getStatus());
            if (request.getStatus().equals(RequestStatus.REJECTED)) {
                req.setStatus(RequestStatus.REJECTED);
            } else if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
                req.setStatus(RequestStatus.CONFIRMED);
                confirmed++;
            } else if (limit == 0) {
                req.setStatus(RequestStatus.REJECTED);
            } else {
                req.setStatus(RequestStatus.CONFIRMED);
                limit--;
            }
            requestRepository.save(req);
        }
        if (event.getParticipantLimit() != 0)
            event.setConfirmedRequests(event.getParticipantLimit() - limit);
        else
            event.setConfirmedRequests(confirmed);
        eventRepository.save(event);
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(requestRepository.findAllByEventId_IdAndStatusAndIdIn(eventId,
                        RequestStatus.CONFIRMED,
                        request.getRequestIds()).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList());
        result.setRejectedRequests(requestRepository.findAllByEventId_IdAndStatusAndIdIn(eventId,
                        RequestStatus.REJECTED,
                        request.getRequestIds()).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList());
        return result;
    }

    @Override
    public Collection<ParticipationRequestDto> findAllRequestsByEventId(Long userId, Long eventId) {
        validateUserExist(userId);
        Collection<ParticipationRequestDto> result = new ArrayList<>();
        result = requestRepository.findAllByEventId_Id(eventId).stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
        return result;
    }

    private User validateUserExist(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователя с id = " + userId + " не существует"));
    }

    private Event validateEventExist(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("События с id = " + eventId + " не существует"));
    }

    private LocalDateTime parseDateTime(String dateTimeStr, LocalDateTime defaultIfNull) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return defaultIfNull;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, Constants.DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss");
        }
    }

    private String mapSortParameterToFieldName(String sort) {
        return switch (sort.toUpperCase()) {
            case "EVENT_DATE" -> "eventDate";
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

    private Event updateUniversal(Event oldEvent, UpdateEventRequest updateEvent) {
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

    private void validateEventDate(LocalDateTime eventDate, LocalDateTime minValidDate) {
        if (eventDate != null && eventDate.isBefore(minValidDate)) {
            throw new ValidationException("Дата события должна быть назначена через два часа");
        }
    }

    private void updateEventViews(Event event, HttpServletRequest request) {
        //sendStat(request);

        LocalDateTime statsStartDate = event.getPublishedOn();
        if (statsStartDate == null) {
            statsStartDate = event.getCreatedOn();
        }
        List<StatsDto> views = statsClient.getStats(
                statsStartDate.format(Constants.DATE_TIME_FORMATTER),
                LocalDateTime.now().format(Constants.DATE_TIME_FORMATTER),
                List.of(request.getRequestURI()),
                true);

        long newViews;
        if (views.isEmpty()) {
            newViews = 1L;
        } else {
            newViews = views.get(0).getHits();
        }
        event.setViews(newViews);
        eventRepository.save(event);
    }

    private Pageable createPageable(int from, int size, String sort) {
        if (sort != null && !sort.isBlank()) {
            String sortField = mapSortParameterToFieldName(sort);
            return PageRequest.of(from, size, Sort.by(sortField).descending());
        }
        return PageRequest.of(from, size);
    }
}
