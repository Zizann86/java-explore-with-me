package ru.practicum.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.service.event.EventService;

import java.util.Collection;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {
   private final EventService eventService;

    @PatchMapping("/{eventId}")
    public EventFullDto updateEventAdmin(@PathVariable Long eventId,
                                         @RequestBody @Valid UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("Получен HTTP-запрос на обновление события с id: {}", eventId);

        return eventService.updateEventAdmin(eventId, updateEventAdminRequest);
    }

    @GetMapping
    public Collection<EventFullDto> getEvents(@RequestParam(required = false) List<Long> users,
                                              @RequestParam(required = false) List<String> states,
                                              @RequestParam(required = false) List<Long> categories,
                                              @RequestParam(required = false) String rangeStart,
                                              @RequestParam(required = false) String rangeEnd,
                                              @RequestParam(defaultValue = "0") Integer from,
                                              @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("Получен HTTP-запрос на получение всех события для admin");
        return eventService.findEventsByAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }
}
