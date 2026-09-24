package zw.co.petrotrade.workflow.fuel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FuelApprovalRequest(
        @NotNull Long requestId,
        @NotNull Long fuelCardId,
        boolean approved,
        @NotBlank String approver,
        String comments) {
}
