package zw.co.petrotrade.workflow.fuel.dto;

import jakarta.validation.constraints.NotNull;

// the approver is the signed-in user
public record FuelApprovalRequest(
        @NotNull Long requestId,
        boolean approved,
        boolean usePersonalCard,
        String comments) {
}
