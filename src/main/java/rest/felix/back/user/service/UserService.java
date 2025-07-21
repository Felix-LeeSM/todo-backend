package rest.felix.back.user.service;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.user.dto.SignupDTO;
import rest.felix.back.user.dto.SignupRequestDTO;
import rest.felix.back.user.dto.UserDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.ConfirmPasswordMismatchException;
import rest.felix.back.user.exception.UsernameTakenException;
import rest.felix.back.user.repository.UserRepository;

@Service
@AllArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserGroupRepository userGroupRepository;

  @Transactional
  public UserDTO signup(SignupDTO signupDTO) {
    User user = userRepository.createUser(signupDTO);

    return new UserDTO(
        user.getId(), user.getNickname(), user.getUsername(), user.getHashedPassword());
  }

  @Transactional(readOnly = true)
  public void validateSignupRequestDTO(SignupRequestDTO signupRequestDTO)
      throws ConfirmPasswordMismatchException, UsernameTakenException {

    if (!signupRequestDTO.getPassword().equals(signupRequestDTO.getConfirmPassword()))
      throw new ConfirmPasswordMismatchException();

    if (userRepository.findByUsername(signupRequestDTO.getUsername()).isPresent())
      throw new UsernameTakenException();
  }

  @Transactional(readOnly = true)
  public Optional<UserDTO> findByUsername(String username) {

    return userRepository.findByUsername(username);
  }

  @Transactional(readOnly = true)
  public Optional<UserDTO> findById(Long id) {

    return userRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public boolean existsById(Long userId) {
    return userRepository.findById(userId).isPresent();
  }
}
