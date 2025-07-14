package rest.felix.back.common.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JwtTokenProviderTest {

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Test
  void generateParseValidate() {
    // Given

    String username = "randomUsernameForTest";
    String token = jwtTokenProvider.generateToken(username);

    // When

    Assertions.assertDoesNotThrow(
        () -> {
          jwtTokenProvider.validateToken(token);
        });

    // Then

    Assertions.assertNotEquals(username, token);
    Assertions.assertEquals(username, jwtTokenProvider.getUsernameFromToken(token));
  }
}
