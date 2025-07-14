package rest.felix.back.group.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateGroupRequestDTO {

  @NotNull
  @Size(max = 50)
  private String name;

  @NotNull
  @Size(max = 200)
  private String description;
}
