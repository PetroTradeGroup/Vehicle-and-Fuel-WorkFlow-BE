package zw.co.petrotrade.workflow.approval;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // what requestId points at: a transport request or a station top-up
    @Enumerated(EnumType.STRING)
    private ApprovalSubject subject;

    private Long requestId;

    private String approver;

    @Enumerated(EnumType.STRING)
    private ApprovalLevel level;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;

    private String comments;

    private LocalDateTime approvalDate;

    public static Approval record(
            ApprovalSubject subject,
            Long requestId,
            ApprovalLevel level,
            boolean approved,
            String approver,
            String comments) {
        Approval approval = new Approval();
        approval.setSubject(subject);
        approval.setRequestId(requestId);
        approval.setLevel(level);
        approval.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        approval.setApprover(approver);
        approval.setComments(comments);
        approval.setApprovalDate(LocalDateTime.now());
        return approval;
    }
}
