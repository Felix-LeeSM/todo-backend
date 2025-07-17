package rest.felix.back.common.security;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class PasswordService {

  private final PasswordEncoder passwordencoder;

  public String hashPassword(String rawPassword) {
    return this.passwordencoder.encode(rawPassword);
  }

  public boolean verifyPassword(String rawPassword, String hashedPassword) {
    return this.passwordencoder.matches(rawPassword, hashedPassword);
  }
}
