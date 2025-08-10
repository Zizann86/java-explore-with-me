package ru.practicum.service.compilation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.dal.CompilationRepository;
import ru.practicum.dal.EventRepository;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto);
        Set<Long> compEventIds;
        if (newCompilationDto.getEvents() != null) {
            compEventIds = newCompilationDto.getEvents();
        } else {
            compEventIds = Collections.emptySet();
        }
        List<Long> eventIds = new ArrayList<>(compEventIds);
        List<Event> events = eventRepository.findAllByIdIn(eventIds);
        Set<Event> eventsSet = new HashSet<>(events);
        compilation.setEvents(eventsSet);

        Compilation compilationAfterSave = compilationRepository.save(compilation);
        return CompilationMapper.toCompilationDto(compilationAfterSave);
    }

    @Override
    public Collection<CompilationDto> getAllCompilations(Boolean pinned, Integer from, Integer size) {
        PageRequest pageRequest = PageRequest.of(from, size);
        List<Compilation> compilations;
        if (pinned == null) {
            compilations = compilationRepository.findAll(pageRequest).getContent();
        } else {
            compilations = compilationRepository.findAllByPinned(pinned, pageRequest);
        }

        return compilations.stream()
                .map(CompilationMapper::toCompilationDto)
                .collect(Collectors.toList());
    }
}