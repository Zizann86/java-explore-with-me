package ru.practicum.service.event;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.Collection;
import java.util.List;


public interface EventService {

    EventFullDto save(Long userId, NewEventDto newEventDto);

    EventFullDto getEventById(Long eventId, HttpServletRequest request);

    EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest);

    List<EventFullDto> findEventsByAdmin(List<Long> users,
                                      List<String> states,
                                      List<Long> categories,
                                      String rangeStart,
                                      String rangeEnd,
                                      int from,
                                      int size);

    List<EventShortDto> findEventsByPublic(String text,
                                                 List<Long> categories,
                                                 Boolean paid,
                                                 String rangeStart,
                                                 String rangeEnd,
                                                 Boolean onlyAvailable,
                                                 String sort,
                                                 int from,
                                                 int size,
                                                 HttpServletRequest request);

    List<EventShortDto> findEvents(Long userId, int from, int size);

    EventFullDto findEvent(Long eventId, Long userId);

    EventFullDto updateEvent(Long eventId, Long userId, UpdateEventUserRequest updateEventUserRequest);

    EventRequestStatusUpdateResult updateRequestsStatus(Long userId,
                                                        Long eventId,
                                                        EventRequestStatusUpdateRequest request);

    Collection<ParticipationRequestDto> findAllRequestsByEventId(Long userId, Long eventId);
}
