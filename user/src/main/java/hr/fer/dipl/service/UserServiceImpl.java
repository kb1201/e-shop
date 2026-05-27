package hr.fer.dipl.service;

import hr.fer.dipl.db.model.Role;
import hr.fer.dipl.db.model.User;
import hr.fer.dipl.db.repository.RoleRepository;
import hr.fer.dipl.db.repository.UserRepository;
import hr.fer.dipl.dto.UserRequest;
import hr.fer.dipl.dto.UserResponse;
import hr.fer.dipl.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    public UserResponse create(final UserRequest rq) {
        String encodedPassword = passwordEncoder.encode(rq.getPassword());
        User user = UserMapper.toEntity(rq);
        user.setPassword(encodedPassword);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role USER not found"));

        user.setRoles(Set.of(userRole));

        User result = userRepository.save(user);
        return UserMapper.toResponse(result);
    }
}
