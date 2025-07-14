package rest.felix.back.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.todo.entity.enumerated.TodoStatus;

@Getter
@AllArgsConstructor
public class UpdateTodoDTO {

  private final long id;
  private final String title;
  private final String description;
  private final String order;
  private final TodoStatus status;
}
