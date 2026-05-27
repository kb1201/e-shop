package hr.fer.dipl.service;

import hr.fer.dipl.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(String username, String password);

    String generateToken(String username, String password);
}
