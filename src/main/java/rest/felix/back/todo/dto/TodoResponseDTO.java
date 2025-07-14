package rest.felix.back.todo.dto;

import rest.felix.back.todo.entity.enumerated.TodoStatus;

public record TodoResponseDTO(
    long id,
    String title,
    String description,
    String order,
    TodoStatus status,
    long authorId,
    long groupId) {
  public static TodoResponseDTO of(TodoDTO todoDTO) {
    return new TodoResponseDTO(
        todoDTO.getId(),
        todoDTO.getTitle(),
        todoDTO.getDescription(),
        todoDTO.getOrder(),
        todoDTO.getStatus(),
        todoDTO.getAuthorId(),
        todoDTO.getGroupId());
  }
}
