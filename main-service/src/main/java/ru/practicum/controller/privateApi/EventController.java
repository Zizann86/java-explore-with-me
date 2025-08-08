package ru.practicum.controller.privateApi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.service.event.EventService;

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
}
