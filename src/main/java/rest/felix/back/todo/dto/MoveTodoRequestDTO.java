package rest.felix.back.todo.dto;

import jakarta.validation.constraints.NotNull;
import rest.felix.back.todo.entity.enumerated.TodoStatus;

public record MoveTodoRequestDTO(@NotNull TodoStatus todoStatus, Long destinationId) {}
