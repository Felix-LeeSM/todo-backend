package rest.felix.back.todo.dto;

import java.time.LocalDate;
import rest.felix.back.todo.entity.enumerated.TodoStatus;

public record TodoWithStarredStatusResponseDTO(
    long id,
    String title,
    String description,
    String order,
    TodoStatus status,
    boolean isImportant,
    LocalDate dueDate,
    boolean isStarred,
    long authorId,
    long groupId,
    Long assigneeId) {

  public static TodoWithStarredStatusResponseDTO of(TodoWithStarredStatusDTO dto) {
    return new TodoWithStarredStatusResponseDTO(
        dto.id(),
        dto.title(),
        dto.description(),
        dto.order(),
        dto.status(),
        dto.isImportant(),
        dto.dueDate(),
        dto.isStarred(),
        dto.authorId(),
        dto.groupId(),
        dto.assigneeId());
  }
}
