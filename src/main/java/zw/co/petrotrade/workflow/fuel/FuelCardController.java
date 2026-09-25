package zw.co.petrotrade.workflow.fuel;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.dto.ApprovalResponse;
import zw.co.petrotrade.workflow.fuel.dto.FuelApprovalRequest;
import zw.co.petrotrade.workflow.fuel.dto.FuelCardRequest;
import zw.co.petrotrade.workflow.fuel.dto.FuelCardResponse;

import java.util.List;

@RestController
@RequestMapping("/api/fuel-cards")
@RequiredArgsConstructor
public class FuelCardController {

    private final FuelCardRepository repository;
    private final FuelCardService fuelCardService;
    private final FuelApprovalService fuelApprovalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FuelCardResponse create(@Valid @RequestBody FuelCardRequest request) {
        return FuelCardResponse.from(fuelCardService.create(request.toEntity()));
    }

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
                request.approved(),
                request.usePersonalCard(),
                request.approver(),
                request.comments());
        return ApprovalResponse.from(approval);
    }
}
