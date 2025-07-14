package rest.felix.back.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateTodoDTO {

  private final String title;
  private final String description;
  private final String order;
  private final long authorId;
  private final long groupId;
}
