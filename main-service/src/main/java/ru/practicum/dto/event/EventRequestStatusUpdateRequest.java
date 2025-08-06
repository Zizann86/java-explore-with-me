package ru.practicum.dto.event;

import lombok.*;
import ru.practicum.model.enums.RequestStatus;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventRequestStatusUpdateRequest {

    List<Long> requestIds;
    RequestStatus status;
}
