package rest.felix.back.todo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateTodoMetadataRequestDTO(
    @NotNull Boolean isImportant, LocalDate dueDate, Long assigneeId) {}
