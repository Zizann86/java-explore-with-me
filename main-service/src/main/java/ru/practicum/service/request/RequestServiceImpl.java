package ru.practicum.service.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dal.EventRepository;
import ru.practicum.dal.RequestRepository;
import ru.practicum.dal.UserRepository;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.Request;
import ru.practicum.model.User;
import ru.practicum.model.enums.RequestStatus;
import ru.practicum.model.enums.State;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        if (requestRepository.findByRequesterIdAndEventId(userId, eventId).isPresent()) {
            throw new ConflictException(String.format(
                    "Запрос от пользователя с id = %d на событие с id = %d уже существует",
                    userId, eventId
            ));
        }
        User user = validateUserExist(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id= " + eventId + " не найдено"));
        LocalDateTime createdOn = LocalDateTime.now();
        if (event.getInitiator().getId() == userId)
            throw new ConflictException("Инициатор события не может отправить запрос");
        if (event.getState() != State.PUBLISHED)
            throw new ConflictException("Событие не опубликовано");
        if (event.getParticipantLimit() != 0 &&
                event.getConfirmedRequests() != null &&
                event.getParticipantLimit() <= event.getConfirmedRequests()) {
            throw new ConflictException("Превышен лимит участников");
        }
        Request request = new Request();
        request.setCreated(createdOn);
        request.setRequester(user);
        request.setEvent(event);

        if (event.getParticipantLimit() != 0 && event.getRequestModeration()) {
            request.setStatus(RequestStatus.PENDING);
        } else {
            request.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }
        return RequestMapper.toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public Collection<ParticipationRequestDto> getRequestsByUserId(Long userId) {
        validateUserExist(userId);
        Collection<Request> result = requestRepository.findAllByRequesterId(userId);
        return result.stream().map(RequestMapper::toParticipationRequestDto).collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        validateUserExist(userId);
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id = " + requestId + " не найден"));
        if (!request.getRequester().getId().equals(userId))
            throw new ConflictException("Пользователь с id = " + userId + " не является инициализатором запроса с id = " + requestId);
        if (request.getStatus().equals(RequestStatus.CANCELED) || request.getStatus().equals(RequestStatus.REJECTED)) {
            throw new ConflictException("Запрос не подтвержден");
        }
        request.setStatus(RequestStatus.CANCELED);
        Request requestAfterSave = requestRepository.save(request);
        return RequestMapper.toParticipationRequestDto(requestAfterSave);
    }

    private User validateUserExist(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id %d не найден.", userId)));
    }
}
