package rest.felix.back.user.dto;

import jakarta.validation.constraints.NotNull;

public record SignInRequestDTO(@NotNull String username, @NotNull String password) {}
