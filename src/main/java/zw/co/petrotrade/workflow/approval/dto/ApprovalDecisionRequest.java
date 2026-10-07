package zw.co.petrotrade.workflow.approval.dto;

import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;

// the approver is the signed-in user
public record ApprovalDecisionRequest(
        @NotNull Long requestId,
        @NotNull ApprovalLevel level,
        boolean approved,
        String comments

) {
}
