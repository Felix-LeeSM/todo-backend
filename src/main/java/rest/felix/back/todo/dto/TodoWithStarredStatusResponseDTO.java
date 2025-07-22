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
        dto.getId(),
        dto.getTitle(),
        dto.getDescription(),
        dto.getOrder(),
        dto.getStatus(),
        dto.isImportant(),
        dto.getDueDate(),
        dto.isStarred(),
        dto.getAuthorId(),
        dto.getGroupId(),
        dto.getAssigneeId());
  }
}
