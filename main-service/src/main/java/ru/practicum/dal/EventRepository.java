package ru.practicum.dal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.Event;
import ru.practicum.model.enums.State;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /*@Query("""
            SELECT e
            FROM Event as e
            WHERE (:text IS NULL OR (e.annotation ilike %:text% OR e.description ilike %:text%))
            AND (:categoryIds IS NULL OR e.category.id in :categoryIds)
            AND (:paid IS NULL OR e.paid = :paid)
            AND (e.eventDate >= :start)
            AND (e.state = :state)
            AND (:onlyAvailable = false OR e.participantLimit = 0 OR e.confirmedRequests <= e.participantLimit)
            AND (CAST(:end AS DATE) IS NULL OR e.eventDate <= :end)
            """)
    List<Event> findAllByFilterPublic(String text,
                                      List<Long> categoryIds,
                                      Boolean paid,
                                      LocalDateTime start,
                                      LocalDateTime end,
                                      Boolean onlyAvailable,
                                      State state,
                                      Pageable pageable);
}*/

    /*@Query("SELECT e FROM Event e " +
            "WHERE e.state = :state " +
            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categoryIds IS NULL OR e.category.id IN :categoryIds) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (:start IS NULL OR e.eventDate >= :start) " +
            "AND (:end IS NULL OR e.eventDate <= :end) " +
            "AND (:onlyAvailable IS NULL OR :onlyAvailable = false " +
            "OR e.participantLimit = 0 OR e.confirmedRequests < e.participantLimit)")
    List<Event> findAllByFilterPublic(@Param("text") String text,
                                      @Param("categoryIds") List<Long> categoryIds,
                                      @Param("paid") Boolean paid,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("onlyAvailable") Boolean onlyAvailable,
                                      @Param("state") State state,
                                      Pageable pageable);
    }*/

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = :state " +
            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) " +
            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categories IS NULL OR e.category_id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (:start IS NULL OR e.event_date >= :start) " +
            "AND (:end IS NULL OR e.event_date <= :end) " +
            "AND (:onlyAvailable = false OR e.participant_limit = 0 OR e.confirmed_requests < e.participant_limit)",
            nativeQuery = true)
    Page<Event> findAllByFilterPublic(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("onlyAvailable") Boolean onlyAvailable,
            @Param("state") String state,
            Pageable pageable);
}
