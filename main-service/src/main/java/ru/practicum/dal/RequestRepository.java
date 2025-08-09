package ru.practicum.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Request;
import ru.practicum.model.enums.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByRequesterId(Long userId);

    List<Request> findAllByEventIdInAndStatus(List<Long> eventIds, RequestStatus status);

    List<Request> findAllByEventId_IdAndIdIn(Long eventId, List<Long> requestIds);

    List<Request> findAllByEventId_IdAndStatusAndIdIn(
            Long eventId,
            RequestStatus status,
            List<Long> requestIds
    );

    List<Request> findAllByEventId_Id(Long eventId);
}
