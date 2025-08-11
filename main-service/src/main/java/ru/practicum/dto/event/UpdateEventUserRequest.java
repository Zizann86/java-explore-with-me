package ru.practicum.dto.event;

import lombok.*;
import ru.practicum.model.enums.ActionState;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventUserRequest extends UpdateEventRequest {
   private ActionState stateAction;
}
