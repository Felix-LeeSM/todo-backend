package rest.felix.back.todo.dto;

import java.time.LocalDate;

public record CreateTodoDTO(
    String title,
    String description,
    LocalDate dueDate,
    long authorId,
    long groupId,
    Long assigneeId) {

  public static CreateTodoDTO of(CreateTodoRequestDTO dto, long authorId, long groupId) {
    return new CreateTodoDTO(
        dto.title(), dto.description(), dto.dueDate(), authorId, groupId, dto.assigneeId());
  }
}
