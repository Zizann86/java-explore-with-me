package ru.practicum.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.dal.UserRepository;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto saveUser(NewUserRequest newUserRequest) {
        validateEmailExists(newUserRequest.getEmail());
        User user = userRepository.save(UserMapper.toFromDto(newUserRequest));
        log.info("Пользователь успешно создан с id: {}", user.getId());
        return UserMapper.fromToDto(user);
    }

    @Override
    public Collection<UserDto> getUsers(List<Long> ids, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        log.info("Пользователи получены");
        return userRepository.findUsersByIds(ids, pageable).stream()
                .map(UserMapper::fromToDto)
                .toList();
    }

    @Override
    public void deleteUser(Long id) {
        validateUserExist(id);
        userRepository.deleteById(id);
        log.info("Пользователь с id: {} успешно удален", id);
    }



    private User validateUserExist(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id %d не найден.", userId)));
    }

    private void validateEmailExists(String email) {
        List<String> emails = userRepository.findAll().stream()
                .map(User::getEmail)
                .toList();
        if (emails.contains(email)) {
            throw new ConflictException("Данная электронная почта уже занята");
        }
    }
}
