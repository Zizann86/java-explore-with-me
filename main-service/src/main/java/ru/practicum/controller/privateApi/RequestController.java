package ru.practicum.controller.privateApi;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.request.RequestService;

import java.util.Collection;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "users/{userId}/requests")
public class RequestController {
    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(@PathVariable(value = "userId") @Min(0) Long userId,
                                              @RequestParam(name = "eventId") @Min(0) Long eventId) {
        log.info("Получен HTTP-запрос на создание запроса на участие в событии с id= {}  пользователя с id= {}",
                eventId, userId);
        return requestService.addRequest(userId, eventId);
    }

    @GetMapping
    public Collection<ParticipationRequestDto> getAllRequests(@PathVariable(value = "userId") @Min(0) Long userId) {
        log.info("Получен HTTP-запрос на получение всех запросов на участие в событиях пользователя с id= {}", userId);
        return requestService.getRequestsByUserId(userId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto canceledRequest(@PathVariable(value = "userId") @Min(0) Long userId,
                                                   @PathVariable(value = "requestId") @Min(0) Long requestId) {
        log.info("Получен HTTP-запрос на отмену запроса пользователем с id= {}", userId);
        return requestService.cancelRequest(userId, requestId);
    }
}
