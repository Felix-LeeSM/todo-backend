package rest.felix.back.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateGroupRequestDTO(@NotBlank String name, @NotNull String description) {}
