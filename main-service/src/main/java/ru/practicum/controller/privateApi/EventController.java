package ru.practicum.controller.privateApi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.service.event.EventService;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable Long userId,
                                 @RequestBody @Valid NewEventDto newEventDto) {
        log.info("Получен HTTP-запрос на добавление нового события от пользователя {} c body: {}", userId, newEventDto);
        return eventService.save(userId, newEventDto);
    }

    @GetMapping("/{userId}/events")
    public Collection<EventShortDto> getEvents(@PathVariable Long userId,
                                               @RequestParam(defaultValue = "0") Integer from,
                                               @RequestParam(defaultValue = "10") Integer size) {
        log.info("Получен HTTP-запрос на получение событий для пользователя {}", userId);
        return eventService.findEvents(userId, from, size);
    }

    @GetMapping("/{userId}/events/{eventId}")
    public EventFullDto getEvent(@PathVariable Long userId,
                             @PathVariable Long eventId) {
        log.info("Получен HTTP-запрос на получение события {} для пользователя {}", eventId, userId);
        return eventService.findEvent(eventId, userId);
    }

    @PatchMapping("/{userId}/events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable Long userId,
                             @PathVariable Long eventId,
                             @RequestBody @Valid UpdateEventUserRequest updateEventUserRequest) {
        log.info("Получен HTTP-запрос на обновление события {} для пользователя {} с body {}", eventId, userId, updateEventUserRequest);
        return eventService.updateEvent(eventId, userId, updateEventUserRequest);
    }
}
