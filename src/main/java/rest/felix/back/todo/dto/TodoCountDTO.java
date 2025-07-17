package rest.felix.back.todo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TodoCountDTO {
  private final long groupId;
  private final long todoCount;
  private final long completedTodoCount;
}
