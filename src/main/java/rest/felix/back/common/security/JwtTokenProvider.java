package rest.felix.back.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rest.felix.back.user.dto.AuthUserDTO;

@Component
public class JwtTokenProvider {

  @Value("${jwt.access_token.secret_key}")
  private String secretKey;

  @Value("${jwt.access_token.ttl}")
  private long expirationTime;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public String generateToken(AuthUserDTO authUser) {
    try {
      String subject = objectMapper.writeValueAsString(authUser);
      return Jwts.builder()
          .setSubject(subject)
          .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
          .signWith(SignatureAlgorithm.HS512, secretKey)
          .compact();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public AuthUserDTO getAuthUserFromToken(String token) {
    try {
      String subject =
          Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
      return objectMapper.readValue(subject, AuthUserDTO.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
