package rest.felix.back.user.service;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.user.dto.SignupDTO;
import rest.felix.back.user.dto.SignupRequestDTO;
import rest.felix.back.user.dto.UserDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.ConfirmPasswordMismatchException;
import rest.felix.back.user.exception.UsernameTakenException;
import rest.felix.back.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager em;
  @Autowired EntityFactory entityFactory;

  @Autowired private TestHelper th;

  @BeforeEach
  void setUp() {
    th.cleanUp();
  }

  @Test
  void signup_HappyPath() {
    // Given

    SignupDTO signupDTO = new SignupDTO("username", "nickname", "hashedPassword");

    // When

    UserDTO createdUserDTO = userService.signup(signupDTO);
    UserDTO createdUser = userRepository.findByUsername("username").get();

    // Then

    Assertions.assertEquals("hashedPassword", createdUserDTO.hashedPassword());
    Assertions.assertEquals("nickname", createdUserDTO.nickname());
    Assertions.assertEquals("username", createdUserDTO.username());
    Assertions.assertEquals(createdUser.id(), createdUserDTO.id());
  }

  @Test
  void signup_Failure_UsernameTaken() {
    // Given

    SignupDTO signupDTO = new SignupDTO("username", "nickname", "hashedPassword");
    SignupDTO duplicatedUsernameSignupDTO =
        new SignupDTO("username", "nickname2", "hashedPassword2");

    // When

    userService.signup(signupDTO);

    // Then

    Assertions.assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          userService.signup(duplicatedUsernameSignupDTO);
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

    // When

    UserDTO userDTO = userService.findByUsername("username").get();

    // Then

    Assertions.assertEquals(user.getId(), userDTO.id());
    Assertions.assertEquals(user.getUsername(), userDTO.username());
    Assertions.assertEquals(user.getNickname(), userDTO.nickname());
    Assertions.assertEquals(user.getHashedPassword(), userDTO.hashedPassword());
  }

  @Test
  void getByUsername_Failure_NoSuchUser() {
    // Given

    // When

    Optional<UserDTO> userDTO = userService.findByUsername("username");

    // Then

    Assertions.assertTrue(userDTO.isEmpty());
  }
}
