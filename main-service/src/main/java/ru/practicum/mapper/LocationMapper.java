package ru.practicum.mapper;

import ru.practicum.dto.location.LocationDto;
import ru.practicum.model.Location;

public class LocationMapper {

    public Location toFromLocationDto(LocationDto locationDto) {
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }

    public LocationDto fromToLocationDto(Location location) {
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}
