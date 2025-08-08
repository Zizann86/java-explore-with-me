package ru.practicum.controller.publicApi;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.service.event.EventService;


@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {
    private final EventService eventService;


    @GetMapping("/{eventId}")
    public EventFullDto getEventById(@PathVariable Long eventId, HttpServletRequest request) {
        log.info("Получен HTTP-запрос на получение события с Id: {} public", eventId);
        return eventService.getEventById(eventId, request);
    }
}
