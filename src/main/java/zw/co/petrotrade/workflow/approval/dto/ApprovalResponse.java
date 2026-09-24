package zw.co.petrotrade.workflow.approval.dto;

import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalStatus;

import java.time.LocalDateTime;

public record ApprovalResponse(
        Long id,
        Long requestId,
        String approver,
        ApprovalLevel level,
        ApprovalStatus status,
        String comments,
        LocalDateTime approvalDate) {

    public static ApprovalResponse from(Approval approval) {
        return new ApprovalResponse(
                approval.getId(),
                approval.getRequestId(),
                approval.getApprover(),
                approval.getLevel(),
                approval.getStatus(),
                approval.getComments(),
                approval.getApprovalDate());
    }
}
