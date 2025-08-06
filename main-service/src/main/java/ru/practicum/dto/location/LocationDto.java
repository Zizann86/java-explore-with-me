package ru.practicum.dto.location;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class LocationDto {
    @NotNull
    @DecimalMin(value = "-90", inclusive = false)
    @DecimalMax(value = "90", inclusive = false)
    private Float lat;

    @NotNull
    @DecimalMin(value = "-180", inclusive = false)
    @DecimalMax(value = "180", inclusive = false)
    private Float lon;
}
