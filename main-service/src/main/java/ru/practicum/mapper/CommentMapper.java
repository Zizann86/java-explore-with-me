package ru.practicum.mapper;

import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.CreateCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;

import java.time.LocalDateTime;

public class CommentMapper {

    public static CommentDto toCommentDto(Comment comment) {
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                comment.getAuthor().getName(),
                comment.getCreated()
        );
    }

   /* public Comment toComment(CreateCommentDto createCommentDto, Event event, User user) {
        return Comment.builder()
                .text(createCommentDto.getText())
                .event(event)
                .author(user)
                .created(LocalDateTime.now())
                .build();
    }*/

    public static Comment toComment(CreateCommentDto createCommentDto) {
        Comment comment = new Comment();
        comment.setText(createCommentDto.getText());
        return comment;
    }

}
