package rest.felix.back.todo.dto;

import java.time.LocalDate;
import rest.felix.back.todo.entity.enumerated.TodoStatus;

public record TodoResponseDTO(
    long id,
    String title,
    String description,
    String order,
    TodoStatus status,
    boolean isImportant,
    LocalDate dueDate,
    long authorId,
    long groupId,
    Long assigneeId) {
  public static TodoResponseDTO of(TodoDTO todoDTO) {
    return new TodoResponseDTO(
        todoDTO.id(),
        todoDTO.title(),
        todoDTO.description(),
        todoDTO.order(),
        todoDTO.status(),
        todoDTO.isImportant(),
        todoDTO.dueDate(),
        todoDTO.authorId(),
        todoDTO.groupId(),
        todoDTO.assigneeId());
  }
}
