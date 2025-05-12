package hr.fer.dipl.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class LoginResponse {
    private String token;

    private long expiresIn;

    private Long id;

}
