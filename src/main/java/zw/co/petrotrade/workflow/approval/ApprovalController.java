package zw.co.petrotrade.workflow.approval;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.approval.dto.ApprovalDecisionRequest;
import zw.co.petrotrade.workflow.approval.dto.ApprovalResponse;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService service;

    @PostMapping
    public ApprovalResponse decide(@Valid @RequestBody ApprovalDecisionRequest request) {
        Approval approval = service.decide(
                request.requestId(),
                request.level(),
                request.approved(),
                request.approver(),
                request.comments());
        return ApprovalResponse.from(approval);
    }
}
