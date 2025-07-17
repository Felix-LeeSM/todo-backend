package rest.felix.back.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.user.entity.User;

@Getter
@AllArgsConstructor
public class UserDTO {

  private final Long id;
  private final String nickname;
  private final String username;
  private final String hashedPassword;

  public static UserDTO of(User user) {
    return new UserDTO(
        user.getId(), user.getNickname(), user.getUsername(), user.getHashedPassword());
  }
}
