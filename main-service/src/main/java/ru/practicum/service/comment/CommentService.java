package ru.practicum.service.comment;

import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.CreateCommentDto;

import java.util.List;

public interface CommentService {

    CommentDto addComment(CreateCommentDto createCommentDto, Long userId, Long eventId);

    List<CommentDto> getCommentsByEvent(Long eventId);

    List<CommentDto> getCommentsByUser(Long userId);

    void deleteComment(Long userId, Long commentId);
}
