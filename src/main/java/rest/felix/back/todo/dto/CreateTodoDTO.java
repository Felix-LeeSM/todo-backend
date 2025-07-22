package rest.felix.back.todo.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateTodoDTO {

  private final String title;
  private final String description;
  private final LocalDate dueDate;
  private final long authorId;
  private final long groupId;
  private final Long assigneeId;

  public static CreateTodoDTO of(CreateTodoRequestDTO dto, long authorId, long groupId) {
    return new CreateTodoDTO(
        dto.getTitle(),
        dto.getDescription(),
        dto.getDueDate(),
        authorId,
        groupId,
        dto.getAssigneeId());
  }
}
