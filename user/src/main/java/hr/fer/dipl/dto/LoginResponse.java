package hr.fer.dipl.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class LoginResponse {

    private long expiresIn;

    private Long id;

    private List<String> roles;

}
