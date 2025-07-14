package rest.felix.back.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignInRequestDTO {

  @NotNull private String username;

  @NotNull private String password;
}
