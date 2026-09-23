package zw.co.petrotrade.workflow.approval;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService service;

    @PostMapping
    public Approval approve(
            @RequestParam Long requestId,
            @RequestParam ApprovalLevel level,
            @RequestParam String approver,
            @RequestParam String comments) {

        return service.approve(
                requestId,
                level,
                approver,
                comments);
    }
}