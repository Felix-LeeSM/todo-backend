package rest.felix.back.todo.dto;

import rest.felix.back.todo.entity.enumerated.TodoStatus;

public record MoveTodoDTO(long todoId, TodoStatus todoStatus, String order) {
  public static MoveTodoDTO of(long todoId, MoveTodoRequestDTO moveTodoRequestDTO) {
    return new MoveTodoDTO(todoId, moveTodoRequestDTO.todoStatus(), moveTodoRequestDTO.order());
  }
}
