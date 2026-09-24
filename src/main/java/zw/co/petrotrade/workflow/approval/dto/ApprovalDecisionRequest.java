package zw.co.petrotrade.workflow.approval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;

public record ApprovalDecisionRequest(
        @NotNull Long requestId,
        @NotNull ApprovalLevel level,
        boolean approved,
        @NotBlank String approver,
        String comments) {
}
