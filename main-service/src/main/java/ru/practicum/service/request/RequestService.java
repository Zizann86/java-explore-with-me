package ru.practicum.service.request;

import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.Collection;

public interface RequestService {

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    Collection<ParticipationRequestDto> getRequestsByUserId(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}
