package hr.fer.dipl.service;


import hr.fer.dipl.db.model.User;
import hr.fer.dipl.db.repository.UserRepository;
import hr.fer.dipl.dto.UserRequest;
import hr.fer.dipl.dto.UserResponse;
import hr.fer.dipl.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse create(final UserRequest rq) {
        String encodedPassword = passwordEncoder.encode(rq.getPassword());
        User user = UserMapper.toEntity(rq);
        user.setPassword(encodedPassword);

        User result = this.userRepository.save(user);
        return UserMapper.toResponse(result);
    }


}