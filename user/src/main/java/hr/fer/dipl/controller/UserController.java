package hr.fer.dipl.controller;


import hr.fer.dipl.dto.LoginResponse;
import hr.fer.dipl.dto.UserRequest;
import hr.fer.dipl.dto.UserResponse;
import hr.fer.dipl.service.AuthService;
import hr.fer.dipl.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//TODO extract services to interfaces
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final AuthService authService;


    @PostMapping()
    public ResponseEntity<UserResponse> createUser(final @Valid @RequestBody UserRequest rq) {
        return new ResponseEntity<>(this.service.create(rq), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(
            @RequestBody UserRequest loginUserDto,
            HttpServletResponse servletResponse) {

        String token = authService.generateToken(loginUserDto.getUsername(), loginUserDto.getPassword());
        LoginResponse loginResponse = authService.login(loginUserDto.getUsername(), loginUserDto.getPassword());

        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (loginResponse.getExpiresIn() / 1000));
        // cookie.setSecure(true); // enable in production (HTTPS)
        servletResponse.addCookie(cookie);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse servletResponse) {
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        servletResponse.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

}
