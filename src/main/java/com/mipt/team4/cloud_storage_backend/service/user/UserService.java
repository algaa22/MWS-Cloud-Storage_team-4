package com.mipt.team4.cloud_storage_backend.service.user;

import com.mipt.team4.cloud_storage_backend.exception.session.InvalidSessionException;
import com.mipt.team4.cloud_storage_backend.exception.user.InvalidEmailOrPassword;
import com.mipt.team4.cloud_storage_backend.exception.user.UserAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.WrongPasswordException;
import com.mipt.team4.cloud_storage_backend.model.user.dto.*;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.security.PasswordHasher;
import java.util.*;

public class UserService {
  private final UserRepository userRepository;
  private final UserSessionService userSessionService;
  public UserService(UserRepository userRepository, UserSessionService userSessionService) {
    this.userRepository = userRepository;
    this.userSessionService = userSessionService;
  }

  public UserEntity getUserInfo(String email) throws UserNotFoundException {
    Optional<UserEntity> userOpt = userRepository.getUserByEmail(email);

    if (userOpt.isEmpty()) {
      throw new UserNotFoundException(email);
    }
    UserEntity user = userOpt.get();
    return new UserEntity(
        null,
        user.getName(),
        user.getEmail(),
        null,
        user.getStorageLimit(),
        user.getUsedStorage(),
        null,
        user.isActive()
    );
  }

  public String registerUser(RegisterRequestDto registerRequest) throws UserAlreadyExistsException {

    if (userRepository.getUserByEmail(registerRequest.email()).isPresent())
      throw new UserAlreadyExistsException(registerRequest.email());

    String hash = PasswordHasher.hash(registerRequest.password());
    UserEntity userEntity =
        new UserEntity(
            UUID.randomUUID(), registerRequest.userName(), registerRequest.email(), hash);

    userRepository.addUser(userEntity);
    SessionDto session = userSessionService.createSession(userEntity);

    return session.token();
  }

  public String loginUser(LoginRequestDto loginRequest)
      throws WrongPasswordException, InvalidEmailOrPassword {
    Optional<UserEntity> userOpt = userRepository.getUserByEmail(loginRequest.email());
    if (userOpt.isEmpty()) throw new InvalidEmailOrPassword();

    UserEntity user = userOpt.get();
    if (!PasswordHasher.verify(loginRequest.password(), user.getPassword())) {
      throw new WrongPasswordException();
    }

    Optional<SessionDto> session = userSessionService.findSessionByEmail(user.getEmail());
    String token;

    if (session.isPresent()) token = session.get().token();
    else token = userSessionService.createSession(user).token();

    // TODO: refresh-токены?
    return token;
  }

  public void logoutUser(LogoutRequestDto logoutRequest) throws UserNotFoundException, InvalidSessionException {
    String token = logoutRequest.token();

    if (userSessionService.tokenExists(token))
      userSessionService.blacklistToken(token);
    else
      throw new UserNotFoundException(token);
  }

  public void updateUserInfo(String token, String newName) throws UserNotFoundException {
    UUID id = userSessionService.extractUserIdFromToken(token);
    Optional<UserEntity> userOpt = userRepository.getUserById(id);
    if (userOpt.isEmpty()) {
      throw new UserNotFoundException(id);
    }
    userRepository.updateInfo(id, newName);
  }
}
