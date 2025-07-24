package rest.felix.back.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rest.felix.back.user.dto.AuthUserDTO;

// AuthUserDTO에 Role이 있다고 가정

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long expirationTime;
  private final JwtParser jwtParser;

  // 1. 생성자 주입 방식으로 변경: 불변성(immutability)을 보장하고 테스트 용이성을 높임.
  public JwtTokenProvider(
      @Value("${jwt.access_token.secret_key}") String secretKey,
      @Value("${jwt.access_token.ttl}") long expirationTime) {

    this.expirationTime = expirationTime;

    // 2. SecretKey와 JwtParser를 애플리케이션 로딩 시점에 한 번만 생성해서 재사용.
    // 이게 보안과 성능 모두에 이득이야.
    this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    this.jwtParser = Jwts.parser().verifyWith(this.key).build();
  }

  public String generateToken(AuthUserDTO authUser) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationTime);

    // 3. Subject에는 JSON 문자열 대신 사용자의 고유 ID만 저장하는 게 표준이야.
    // 복잡한 정보는 별도의 private claim으로 추가.
    return Jwts.builder()
        .subject(authUser.getUserId().toString())
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(key)
        .compact();
  }

  public AuthUserDTO getAuthUserFromToken(String token) {
    Claims claims = jwtParser.parseSignedClaims(token).getPayload();
    Long userId = Long.parseLong(claims.getSubject());

    return new AuthUserDTO(userId);
  }

  public boolean validateToken(String token) {
    try {
      jwtParser.parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
