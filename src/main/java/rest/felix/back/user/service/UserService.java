package rest.felix.back.user.service;

import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import rest.felix.back.common.exception.throwable.badrequest.ConfirmPasswordMismatchException;
import rest.felix.back.common.exception.throwable.badrequest.UsernameTakenException;
import rest.felix.back.common.exception.throwable.unauthorized.NoMatchingUserException;
import rest.felix.back.user.dto.SignupDTO;
import rest.felix.back.user.dto.SignupRequestDTO;
import rest.felix.back.user.dto.UserDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.repository.UserRepository;

@Service
@Transactional
@AllArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public UserDTO signup(SignupDTO signupDTO) {
    User user = userRepository.createUser(signupDTO);

    return new UserDTO(
        user.getId(), user.getNickname(), user.getUsername(), user.getHashedPassword());
  }

  public void validateSignupRequestDTO(SignupRequestDTO signupRequestDTO)
      throws ConfirmPasswordMismatchException, UsernameTakenException {

    if (!signupRequestDTO.getPassword().equals(signupRequestDTO.getConfirmPassword())) {
      throw new ConfirmPasswordMismatchException();
    }

    if (userRepository.getByUsername(signupRequestDTO.getUsername()).isPresent()) {
      throw new UsernameTakenException();
    }
  }

  public Optional<UserDTO> getByUsername(String username) throws NoMatchingUserException {

    return userRepository
        .getByUsername(username)
        .map(
            user ->
                new UserDTO(
                    user.getId(),
                    user.getNickname(),
                    user.getUsername(),
                    user.getHashedPassword()));
  }
}
