package ru.practicum.service.event;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.NewEventDto;


public interface EventService {

    EventFullDto save(Long userId, NewEventDto newEventDto);

    EventFullDto getEventById(Long eventId, HttpServletRequest request);
}
