package rest.felix.back.user.service;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.user.dto.SignupDTO;
import rest.felix.back.user.dto.SignupRequestDTO;
import rest.felix.back.user.dto.UserDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.ConfirmPasswordMismatchException;
import rest.felix.back.user.exception.UsernameTakenException;
import rest.felix.back.user.repository.UserRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserServiceTest {

  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager em;
  @Autowired private PasswordService passwordService;
  private EntityFactory entityFactory;

  @BeforeEach
  void setUp() {
    entityFactory = new EntityFactory(passwordService, em);
  }

  @Test
  void signup_HappyPath() {
    // Given

    SignupDTO signupDTO = new SignupDTO("username", "nickname", "hashedPassword");

    // When

    UserDTO createdUserDTO = userService.signup(signupDTO);
    User createdUser = userRepository.getByUsername("username").get();

    // Then

    Assertions.assertEquals("hashedPassword", createdUserDTO.getHashedPassword());
    Assertions.assertEquals("nickname", createdUserDTO.getNickname());
    Assertions.assertEquals("username", createdUserDTO.getUsername());
    Assertions.assertEquals(createdUser.getId(), createdUserDTO.getId());
  }

  @Test
  void signup_Failure_UsernameTaken() {
    // Given

    SignupDTO signupDTO = new SignupDTO("username", "nickname", "hashedPassword");
    SignupDTO duplicatedUsernameSignupDTO =
        new SignupDTO("username", "nickname2", "hashedPassword2");

    // When

    userService.signup(signupDTO);
    em.flush();

    // Then

    Assertions.assertThrows(
        ConstraintViolationException.class,
        () -> {
          userService.signup(duplicatedUsernameSignupDTO);
          em.flush();
        });
  }

  @Test
  void validateSignupRequestDTO_HappyPath() {
    // Given

    // When

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("username", "nickname", "password", "password");

    // Then

    Assertions.assertDoesNotThrow(
        () -> {
          userService.validateSignupRequestDTO(signupRequestDTO);
        });
  }

  @Test
  void validateSignupRequestDTO_UsernameTaken() {
    // Given

    entityFactory.insertUser("username", "hashedPassword", "nickname");

    // When

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("username", "nickname", "password", "password");

    // Then

    Assertions.assertThrows(
        UsernameTakenException.class,
        () -> {
          userService.validateSignupRequestDTO(signupRequestDTO);
        });
  }

  @Test
  void validateSignupRequestDTO_PasswordMismatch() {
    // Given

    // When

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("username", "nickname", "password", "passwordMismatch");

    // Then

    Assertions.assertThrows(
        ConfirmPasswordMismatchException.class,
        () -> {
          userService.validateSignupRequestDTO(signupRequestDTO);
        });
  }

  @Test
  void getByUsername_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "password", "nickname");
    em.flush();

    // When

    UserDTO userDTO = userService.getByUsername("username").get();

    // Then

    Assertions.assertEquals(user.getId(), userDTO.getId());
    Assertions.assertEquals(user.getUsername(), userDTO.getUsername());
    Assertions.assertEquals(user.getNickname(), userDTO.getNickname());
    Assertions.assertEquals(user.getHashedPassword(), userDTO.getHashedPassword());
  }

  @Test
  void getByUsername_Failure_NoSuchUser() {
    // Given

    // When

    Optional<UserDTO> userDTO = userService.getByUsername("username");

    // Then

    Assertions.assertTrue(userDTO.isEmpty());
  }
}
