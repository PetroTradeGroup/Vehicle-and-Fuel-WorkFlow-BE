package zw.co.petrotrade.workflow.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;

public record UserCreateRequest(
        @NotBlank String username,
        // temporary password for a new Keycloak login; not needed if the username already has one
        String password,
        String employeeNumber,
        @NotBlank String fullName,
        String email,
        Long departmentId,
        String licenceNumber,
        String jobTitle,
        @NotNull Role role,
        // required for a station admin: the station they run
        Long stationId) {

    public User toEntity() {
        User user = new User();
        user.setUsername(username);
        user.setEmployeeNumber(employeeNumber);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setLicenceNumber(licenceNumber);
        user.setJobTitle(jobTitle);
        user.setRole(role);
        user.setStationId(role == Role.STATION_ADMIN ? stationId : null);
        return user;
    }
}
