package ru.practicum.dto.event;

import lombok.*;
import ru.practicum.model.enums.ActionStateAdmin;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateEventAdminRequest extends UpdateEventRequest {
    private ActionStateAdmin stateAction;

}
