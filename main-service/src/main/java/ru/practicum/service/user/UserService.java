package ru.practicum.service.user;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.Collection;
import java.util.List;

public interface UserService {

    UserDto saveUser(NewUserRequest newUserRequest);

    Collection<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long id);
}
