package hr.fer.dipl.service;

import hr.fer.dipl.dto.UserRequest;
import hr.fer.dipl.dto.UserResponse;

public interface UserService {

    UserResponse create(UserRequest rq);
}
