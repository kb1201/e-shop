package hr.fer.dipl.mapper;

import hr.fer.dipl.db.model.User;
import hr.fer.dipl.dto.UserRequest;
import hr.fer.dipl.dto.UserResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserMapper {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static User toEntity(final UserRequest dto) {
        return User.builder()
                .username(dto.getUsername())
                .password(encoder.encode(dto.getPassword()))
                .build();
    }

    public static UserResponse toResponse(final User entity) {
        return UserResponse.builder()
                .username(entity.getUsername())
                .id(entity.getId())
                .build();
    }

    public static UserResponse toResponseSimple(final User entity) {
        return UserResponse.builder()
                .username(entity.getUsername())
                .id(entity.getId())
                .build();
    }
}