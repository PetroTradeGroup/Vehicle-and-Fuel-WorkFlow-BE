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

    private Long requestId;

    private String approver;

    @Enumerated(EnumType.STRING)
    private ApprovalLevel level;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;

    private String comments;

    private LocalDateTime approvalDate;
}