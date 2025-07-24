package rest.felix.back.todo.dto;

import java.time.LocalDate;

public record UpdateTodoMetadataDTO(
    long todoId, boolean isImportant, LocalDate dueDate, Long assigneeId) {}
