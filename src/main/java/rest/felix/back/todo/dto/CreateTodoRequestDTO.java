package rest.felix.back.todo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateTodoRequestDTO {

  @NotNull
  @Size(max = 50)
  private String title;

  @NotNull
  @Size(max = 200)
  private String description;

  @NotEmpty private String order;
}
