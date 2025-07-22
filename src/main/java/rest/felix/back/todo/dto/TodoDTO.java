package rest.felix.back.todo.dto;

import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TodoDTO {

  private final long id;
  private final String title;
  private final String description;
  private final String order;
  private final TodoStatus status;
  private final boolean isImportant;
  private final LocalDate dueDate;
  private final long authorId;
  private final long groupId;
  private final Long assigneeId;

  public static TodoDTO of(Todo todo) {
    return new TodoDTO(
        todo.getId(),
        todo.getTitle(),
        todo.getDescription(),
        todo.getOrder(),
        todo.getTodoStatus(),
        todo.isImportant(),
        todo.getDueDate(),
        todo.getAuthor().getId(),
        todo.getGroup().getId(),
        todo.getAssignee() != null ? todo.getAssignee().getId() : null);
  }
}
