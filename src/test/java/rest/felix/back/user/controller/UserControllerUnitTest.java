package rest.felix.back.user.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.security.JwtTokenProvider;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.user.dto.SignInRequestDTO;
import rest.felix.back.user.dto.SignupRequestDTO;
import rest.felix.back.user.dto.UserResponseDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.ConfirmPasswordMismatchException;
import rest.felix.back.user.exception.NoMatchingUserException;
import rest.felix.back.user.exception.UsernameTakenException;
import rest.felix.back.user.repository.UserRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserControllerUnitTest {

  @Autowired private UserController userController;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager em;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private PasswordService passwordService;
  private EntityFactory entityFactory;

  @BeforeEach
  void setUp() {
    entityFactory = new EntityFactory(passwordService, em);
  }

  @Test
  void signUp_HappyPath() {
    // Given

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("username", "nickname", "password", "password");

    // When

    ResponseEntity<UserResponseDTO> response = userController.signUp(signupRequestDTO);

    // Then

    UserResponseDTO signupResponseDTO = response.getBody();

    Assertions.assertNotNull(signupResponseDTO.id());
    Assertions.assertEquals("username", signupResponseDTO.username());
    Assertions.assertEquals("nickname", signupResponseDTO.nickname());

    User user = userRepository.getById(signupResponseDTO.id()).get();
    Assertions.assertEquals("username", user.getUsername());
    Assertions.assertEquals("nickname", user.getNickname());
  }

  @Test
  void signUp_Failure_UsernameDuplicated() {

    // Given

    entityFactory.insertUser("duplicateUsername", "hashed_password", "nickname1");

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("duplicateUsername", "nickname2", "password", "password");

    // When

    Assertions.assertThrows(
        UsernameTakenException.class,
        () -> {
          userController.signUp(signupRequestDTO);
        });
  }

  @Test
  void signUp_Failure_ConfirmPasswordMismatch() {
    // Given

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("duplicateUsername", "nickname2", "password", "mismatchPassword");

    // When

    Assertions.assertThrows(
        ConfirmPasswordMismatchException.class,
        () -> {
          userController.signUp(signupRequestDTO);
        });
  }

  @Test
  void createAccessToken_HappyPath() {
    // Given

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("username", "nickname", "password", "password");

    UserResponseDTO user = userController.signUp(signupRequestDTO).getBody();

    SignInRequestDTO signInRequestDTO = new SignInRequestDTO("username", "password");

    // When

    ResponseEntity<UserResponseDTO> response = userController.createAccessToken(signInRequestDTO);

    // Then

    UserResponseDTO userResponseDTO = response.getBody();

    Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    Assertions.assertEquals(user.id(), userResponseDTO.id());
    Assertions.assertEquals(user.username(), userResponseDTO.username());
    Assertions.assertEquals(user.nickname(), userResponseDTO.nickname());
  }

  @Test
  void createAccessToken_Failure_NoSuchUsername() {
    // Given

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("username", "nickname", "password", "password");

    UserResponseDTO user = userController.signUp(signupRequestDTO).getBody();

    // When

    SignInRequestDTO signInRequestDTO = new SignInRequestDTO("wrong_username", "password");

    // Then

    Assertions.assertThrows(
        NoMatchingUserException.class,
        () -> {
          userController.createAccessToken(signInRequestDTO);
        });
  }

  @Test
  void createAccessToken_Failure_WrongPassword() {
    // Given

    SignupRequestDTO signupRequestDTO =
        new SignupRequestDTO("username", "nickname", "password", "password");

    UserResponseDTO user = userController.signUp(signupRequestDTO).getBody();

    // When

    SignInRequestDTO signInRequestDTO = new SignInRequestDTO("username", "wrong_password");

    // Then

    Assertions.assertThrows(
        NoMatchingUserException.class,
        () -> {
          userController.createAccessToken(signInRequestDTO);
        });
  }

  @Test
  void logOutUser_HappyPath() {
    // Given

    // When

    ResponseEntity response = userController.logOutUser();

    // Then

    Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void currentUserInfo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
    em.flush();

    String token = jwtTokenProvider.generateToken("username");

    // When

    ResponseEntity<UserResponseDTO> response = userController.getCurrentUserInfo(token);

    // Then

    Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

    UserResponseDTO responseDTO = response.getBody();

    Assertions.assertEquals("username", responseDTO.username());
    Assertions.assertEquals("nickname", responseDTO.nickname());
    Assertions.assertEquals("username", responseDTO.username());
    Assertions.assertEquals(user.getId(), responseDTO.id());
  }

  @Test
  void currentUserInfo_Failure_NoToken() {
    // Given

    String token = null;

    // When

    // Then

    Assertions.assertThrows(
        NoMatchingUserException.class,
        () -> {
          userController.getCurrentUserInfo(token);
        });
  }
}
