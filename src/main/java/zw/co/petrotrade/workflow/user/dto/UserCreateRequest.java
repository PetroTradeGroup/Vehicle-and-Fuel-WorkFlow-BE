package zw.co.petrotrade.workflow.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String password,
        String employeeNumber,
        @NotBlank String fullName,
        String email,
        String department,
        String licenceNumber,
        String jobTitle,
        @NotNull Role role) {

    public User toEntity() {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmployeeNumber(employeeNumber);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setDepartment(department);
        user.setLicenceNumber(licenceNumber);
        user.setJobTitle(jobTitle);
        user.setRole(role);
        return user;
    }
}
