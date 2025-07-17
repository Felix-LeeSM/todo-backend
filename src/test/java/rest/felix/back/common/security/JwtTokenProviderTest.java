package rest.felix.back.common.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rest.felix.back.user.dto.AuthUserDTO;

@SpringBootTest
class JwtTokenProviderTest {

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Test
  void generateParseValidate() {
    // Given

    AuthUserDTO authUser = new AuthUserDTO(1L, "username");
    String token = jwtTokenProvider.generateToken(authUser);

    // When

    Assertions.assertDoesNotThrow(
        () -> {
          jwtTokenProvider.validateToken(token);
        });

    // Then

    Assertions.assertNotEquals(authUser.getUsername(), token);
    Assertions.assertEquals(
        authUser.getUsername(), jwtTokenProvider.getAuthUserFromToken(token).getUsername());
  }
}
