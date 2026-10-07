package zw.co.petrotrade.workflow.department.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(
        @NotBlank String name,
        // omitted on create: a new department is active
        Boolean active) {
}
