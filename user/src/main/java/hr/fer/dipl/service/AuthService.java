package hr.fer.dipl.service;


import hr.fer.dipl.db.model.User;
import hr.fer.dipl.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    public LoginResponse login(String username, String password) {
        User user = (User) userDetailsService.loadUserByUsername(username);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);
        authenticationManager.authenticate(authenticationToken);

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return LoginResponse.builder()
                .expiresIn(jwtService.getExpirationTime())
                .id(user.getId())
                .roles(roles)
                .build();
    }

    public String generateToken(String username, String password) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        return jwtService.generateToken(user);
    }
}
