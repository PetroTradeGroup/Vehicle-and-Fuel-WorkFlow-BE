package zw.co.petrotrade.workflow.department.dto;

public record DepartmentResponse(
        Long id,
        String name,
        boolean active,
        // the one head of department, if there is one
        Long headId,
        String headName,
        long members) {
}
