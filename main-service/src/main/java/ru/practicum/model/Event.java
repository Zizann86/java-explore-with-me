package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.model.enums.State;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "events")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "annotation")
    String annotation;

    @ManyToOne
    @JoinColumn(name = "category_id")
    Category category;

    @Column(name = "confirmed_requests")
    @Builder.Default
    Integer confirmedRequests = 0;

    @Column(name = "created_on")
    LocalDateTime createdOn;

    @Column(name = "description")
    String description;

    @Column(name = "event_date")
    LocalDateTime eventDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User initiator;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "location_id")
    Location location;

    @Column(name = "paid")
    Boolean paid;

    @Column(name = "participant_limit")
    Integer participantLimit;

    @Column(name = "published_on")
    LocalDateTime publishedOn;

    @Column(name = "request_moderation")
    Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    State state;

    @Column(name = "title")
    String title;

    @Column(name = "views")
    Long views;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Comment> comments = new HashSet<>();
}
