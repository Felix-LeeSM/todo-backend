package rest.felix.back.todo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTodoRequestDTO(
    @NotEmpty @Size(max = 50) String title, @NotNull @Size(max = 200) String description) {}
