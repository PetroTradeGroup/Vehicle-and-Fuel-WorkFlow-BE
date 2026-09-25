package zw.co.petrotrade.workflow.fuel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FuelApprovalRequest(
        @NotNull Long requestId,
        boolean approved,
        boolean usePersonalCard,
        @NotBlank String approver,
        String comments) {
}
