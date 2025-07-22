package rest.felix.back.todo.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateTodoMetadataDTO {

  private final long todoId;
  private final boolean isImportant;
  private final LocalDate dueDate;
  private final Long assigneeId;
}
