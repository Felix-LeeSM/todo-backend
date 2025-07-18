package rest.felix.back.todo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import rest.felix.back.todo.entity.enumerated.TodoStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MoveTodoRequestDTO {
  @NotNull private TodoStatus todoStatus;
  private Long destinationId;
}
