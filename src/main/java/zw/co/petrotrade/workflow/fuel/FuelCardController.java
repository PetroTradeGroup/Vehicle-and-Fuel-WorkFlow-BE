package zw.co.petrotrade.workflow.fuel;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.dto.ApprovalResponse;
import zw.co.petrotrade.workflow.fuel.dto.FuelApprovalRequest;
import zw.co.petrotrade.workflow.fuel.dto.FuelCardResponse;

import java.util.List;

@RestController
@RequestMapping("/api/fuel-cards")
@RequiredArgsConstructor
public class FuelCardController {

    private final FuelCardRepository repository;
    private final FuelApprovalService fuelApprovalService;

    @GetMapping
    public List<FuelCardResponse> list() {
        return repository.findAll().stream().map(FuelCardResponse::from).toList();
    }

    @GetMapping("/{id}")
    public FuelCardResponse get(@PathVariable Long id) {
        return FuelCardResponse.from(repository.findById(id).orElseThrow());
    }

    @PostMapping("/approve-fuel")
    public ApprovalResponse approveFuel(@Valid @RequestBody FuelApprovalRequest request) {
        Approval approval = fuelApprovalService.decide(
                request.requestId(),
                request.fuelCardId(),
                request.approved(),
                request.approver(),
                request.comments());
        return ApprovalResponse.from(approval);
    }
}
