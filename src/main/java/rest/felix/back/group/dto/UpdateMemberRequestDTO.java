package rest.felix.back.group.dto;

import jakarta.validation.constraints.NotNull;
import rest.felix.back.group.entity.enumerated.GroupRole;

public record UpdateMemberRequestDTO(@NotNull GroupRole role) {}
