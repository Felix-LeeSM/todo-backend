package rest.felix.back.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRequestDTO {

  @NotNull
  @Size(min = 3, max = 50)
  private String username;

  @NotNull
  @Size(min = 3, max = 100)
  private String nickname;

  @NotNull
  @Size(min = 10, max = 100)
  private String password;

  @NotNull
  @Size(min = 10, max = 100)
  private String confirmPassword;
}
