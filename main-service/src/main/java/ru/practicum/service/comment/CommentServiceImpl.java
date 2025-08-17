package ru.practicum.service.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dal.CommentRepository;
import ru.practicum.dal.EventRepository;
import ru.practicum.dal.UserRepository;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.CreateCommentDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public CommentDto addComment(CreateCommentDto createCommentDto, Long userId, Long eventId) {
        User user = validateUserExist(userId);
        Event event = validateEventExist(eventId);
        validateCommentIsEmpty(createCommentDto);
        Comment comment = CommentMapper.toComment(createCommentDto);
        comment.setAuthor(user);
        comment.setEvent(event);
        comment.setCreated(LocalDateTime.now());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Override
    public List<CommentDto> getCommentsByEvent(Long eventId) {
        return commentRepository.findAllByEventId(eventId).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsByUser(Long userId) {
        return commentRepository.findAllByAuthorId(userId).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = validateCommentExist(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ValidationException("Пользователь с id=" + userId + " не является автором комментария с id=" + commentId);
        }
        commentRepository.delete(comment);
    }

    private User validateUserExist(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id %d не найден.", userId)));
    }

    private Event validateEventExist(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("События с id = " + eventId + " не существует"));
    }

    private Comment validateCommentExist(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));
    }

    private void validateCommentIsEmpty(CreateCommentDto createCommentDto) {
        if (createCommentDto.getText().isEmpty()) {
            throw new ValidationException("Комментарий не может быть пустым");
        }
    }
}
