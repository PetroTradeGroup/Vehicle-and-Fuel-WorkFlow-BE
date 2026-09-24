package zw.co.petrotrade.workflow.user.dto;

import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;

public record UserResponse(
        Long id,
        String username,
        String employeeNumber,
        String fullName,
        String email,
        String department,
        String licenceNumber,
        String jobTitle,
        Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmployeeNumber(),
                user.getFullName(),
                user.getEmail(),
                user.getDepartment(),
                user.getLicenceNumber(),
                user.getJobTitle(),
                user.getRole());
    }
}
