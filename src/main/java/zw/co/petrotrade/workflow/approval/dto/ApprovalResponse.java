package zw.co.petrotrade.workflow.approval.dto;

import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalStatus;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;

import java.time.LocalDateTime;

public record ApprovalResponse(
        Long id,
        ApprovalSubject subject,
        Long requestId,
        String approver,
        ApprovalLevel level,
        ApprovalStatus status,
        String comments,
        LocalDateTime approvalDate) {

    public static ApprovalResponse from(Approval approval) {
        return new ApprovalResponse(
                approval.getId(),
                approval.getSubject(),
                approval.getRequestId(),
                approval.getApprover(),
                approval.getLevel(),
                approval.getStatus(),
                approval.getComments(),
                approval.getApprovalDate());
    }
}
