package rest.felix.back.user.dto;

import rest.felix.back.user.entity.User;

public record UserDTO(Long id, String nickname, String username, String hashedPassword) {

  public static UserDTO of(User user) {
    return new UserDTO(
        user.getId(), user.getNickname(), user.getUsername(), user.getHashedPassword());
  }
}
