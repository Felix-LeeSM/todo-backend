package rest.felix.back.user.controller;

import jakarta.validation.Valid;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rest.felix.back.common.exception.throwable.unauthorized.UnauthorizedException;
import rest.felix.back.common.security.JwtTokenProvider;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.user.dto.*;
import rest.felix.back.user.exception.NoMatchingUserException;
import rest.felix.back.user.service.UserService;

@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
public class UserController {

  private final UserService userService;
  private final PasswordService passwordService;
  private final JwtTokenProvider jwtTokenProvider;

  @PostMapping
  public ResponseEntity<UserResponseDTO> signUp(
      @RequestBody @Valid SignupRequestDTO signupRequestDTO) {

    userService.validateSignupRequestDTO(signupRequestDTO);
    String hashedPassword = passwordService.hashPassword(signupRequestDTO.password());

    SignupDTO signupDTO =
        new SignupDTO(signupRequestDTO.username(), signupRequestDTO.nickname(), hashedPassword);

    UserDTO createdUserDTO = userService.signup(signupDTO);

    UserResponseDTO userResponseDTO =
        new UserResponseDTO(
            createdUserDTO.id(), createdUserDTO.username(), createdUserDTO.nickname());

    return ResponseEntity.status(201).body(userResponseDTO);
  }

  @PostMapping("/token/access-token")
  public ResponseEntity<UserResponseDTO> createAccessToken(
      @RequestBody @Valid SignInRequestDTO signInRequestDTO) {

    String givenUsername = signInRequestDTO.username();
    String givenPassword = signInRequestDTO.password();

    UserDTO userDTO =
        userService
            .findByUsername(givenUsername)
            .filter(DTO -> passwordService.verifyPassword(givenPassword, DTO.hashedPassword()))
            .orElseThrow(NoMatchingUserException::new);

    String token = jwtTokenProvider.generateToken(AuthUserDTO.of(userDTO));

    ResponseCookie authCookie =
        ResponseCookie.from("accessToken", token)
            .path("/")
            .httpOnly(true)
            .secure(false)
            .maxAge(Duration.ofHours(24))
            .sameSite("Strict")
            .build();

    return ResponseEntity.status(201)
        .header(HttpHeaders.SET_COOKIE, authCookie.toString())
        .body(new UserResponseDTO(userDTO.id(), userDTO.username(), userDTO.nickname()));
  }

  @DeleteMapping("/token")
  public ResponseEntity<Void> logOutUser() {
    ResponseCookie emptyCookie =
        ResponseCookie.from("accessToken", "")
            .path("/")
            .httpOnly(true)
            .secure(false)
            .maxAge(0)
            .sameSite("Strict")
            .build();

    return ResponseEntity.status(204)
        .header(HttpHeaders.SET_COOKIE, emptyCookie.toString())
        .build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponseDTO> getCurrentUserInfo(
      @AuthenticationPrincipal AuthUserDTO authUser) {

    if (authUser == null) {
      throw new UnauthorizedException("User not authenticated.");
    }

    return userService
        .findById(authUser.getUserId())
        .map(userDTO -> new UserResponseDTO(userDTO.id(), userDTO.username(), userDTO.nickname()))
        .map(ResponseEntity::ok)
        .orElseThrow(NoMatchingUserException::new);
  }
}
