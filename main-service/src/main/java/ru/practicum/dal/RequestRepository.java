package ru.practicum.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.Request;
import ru.practicum.model.enums.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByRequesterId(Long userId);

    List<Request> findAllByEventIdInAndStatus(List<Long> eventIds, RequestStatus status);

    @Query("""
            SELECT r
            FROM Request r
            WHERE r.event.id = :eventId
            AND r.id IN :requestIds
            """)
    List<Request> findRequestsByEventAndIds(Long eventId, List<Long> requestIds);

    @Query("""
            SELECT r
            FROM Request r
            WHERE r.event.id = :eventId
            AND r.status = :status
            AND r.id IN :requestIds
            """)
    List<Request> findRequestsByEventStatusAndIds(Long eventId, RequestStatus status, List<Long> requestIds);

    @Query("""
            SELECT r
            FROM Request r
            WHERE r.event.id = :eventId
            """)
    List<Request> findRequestsByEvent(Long eventId);

    @Query("""
            SELECT r
            FROM Request r
            WHERE r.requester.id = :requesterId
            AND r.event.id = :eventId
            """)
    Optional<Request> findRequestByRequesterAndEvent(Long requesterId, Long eventId);
}
