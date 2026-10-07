package zw.co.petrotrade.workflow.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.user.Role;

// username can't change: it's the Keycloak login
public record UserUpdateRequest(
        String employeeNumber,
        @NotBlank String fullName,
        String email,
        Long departmentId,
        String licenceNumber,
        String jobTitle,
        @NotNull Role role,
        // required for a station admin: the station they run
        Long stationId) {
}
