package ru.practicum.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.StatsDto;
import ru.practicum.model.Hit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<Hit, Long> {

    @Query(value = """
        SELECT new ru.practicum.StatsDto(h.app, h.uri, COUNT(DISTINCT h.ip) AS quantity)
        FROM Hit h
        WHERE h.timestamp BETWEEN :start AND :end
        AND (:uris IS NULL OR h.uri IN :uris)
        GROUP BY h.app, h.uri
        ORDER BY quantity DESC
    """)
    List<StatsDto> findUniqueStatsByUrisAndTimestampBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris
    );

    @Query(value = """
        SELECT new ru.practicum.StatsDto(h.app, h.uri, COUNT(h.ip) AS quantity)
        FROM Hit h
        WHERE h.timestamp BETWEEN :start AND :end
        AND (:uris IS NULL OR h.uri IN :uris)
        GROUP BY h.app, h.uri
        ORDER BY quantity DESC
    """)
    List<StatsDto> findStatsByUrisAndTimestampBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris
    );
}
