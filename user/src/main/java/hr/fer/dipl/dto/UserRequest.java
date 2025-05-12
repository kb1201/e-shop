package hr.fer.dipl.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserRequest {
    @Email(message = "Username must be a valid email address")
    private String username;

    @Pattern(regexp = "^(?=.*[0-9])(?=.*[A-Z]).{8,}$", message = "Password must contain at least one number, one uppercase letter, and be longer than 8 characters")
    private String password;
}