package rest.felix.back.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateTodoDTO {

  private final long id;
  private final String title;
  private final String description;
}
