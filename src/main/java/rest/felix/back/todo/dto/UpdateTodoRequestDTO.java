package rest.felix.back.todo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.todo.entity.enumerated.TodoStatus;

@Getter
@AllArgsConstructor
public class UpdateTodoRequestDTO {

  @NotEmpty
  @Size(max = 50)
  private String title;

  @NotNull
  @Size(max = 200)
  private String description;

  @NotNull private TodoStatus status;

  @NotNull private String order;
}
