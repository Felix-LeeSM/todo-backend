package rest.felix.back.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequestDTO(
    @NotNull @Size(min = 3, max = 50) String username,
    @NotNull @Size(min = 3, max = 100) String nickname,
    @NotNull @Size(min = 10, max = 100) String password,
    @NotNull @Size(min = 10, max = 100) String confirmPassword) {}
