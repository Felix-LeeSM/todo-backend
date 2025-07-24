package rest.felix.back.group.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGroupRequestDTO(
    @NotNull @Size(max = 50) String name, @NotNull @Size(max = 200) String description) {}
