package rest.felix.back.todo.dto;

import java.time.LocalDate;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;

public record TodoWithStarredStatusDTO(
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

  public static TodoWithStarredStatusDTO of(Todo todo, boolean isStarred) {
    return new TodoWithStarredStatusDTO(
        todo.getId(),
        todo.getTitle(),
        todo.getDescription(),
        todo.getOrder(),
        todo.getTodoStatus(),
        todo.isImportant(),
        todo.getDueDate(),
        isStarred,
        todo.getAuthor().getId(),
        todo.getGroup().getId(),
        todo.getAssignee() != null ? todo.getAssignee().getId() : null);
  }
}
