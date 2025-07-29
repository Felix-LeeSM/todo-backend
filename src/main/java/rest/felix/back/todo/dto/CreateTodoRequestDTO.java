package rest.felix.back.todo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTodoRequestDTO(
    @NotNull @Size(max = 100) String title,
    @NotNull @Size(max = 1000) String description,
    LocalDate dueDate,
    Long assigneeId) {}
