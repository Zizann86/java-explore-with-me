package ru.practicum.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findAllByEventId(Long eventId);

    List<Comment> findAllByAuthorId(Long userId);
}
