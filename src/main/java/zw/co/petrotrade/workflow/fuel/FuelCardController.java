package zw.co.petrotrade.workflow.fuel;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.dto.ApprovalResponse;
import zw.co.petrotrade.workflow.fuel.dto.FuelApprovalRequest;
import zw.co.petrotrade.workflow.fuel.dto.FuelCardRequest;
import zw.co.petrotrade.workflow.fuel.dto.FuelCardResponse;
import zw.co.petrotrade.workflow.security.CurrentUser;

import java.util.List;

@RestController
@RequestMapping("/api/fuel-cards")
@RequiredArgsConstructor
public class FuelCardController {

    private final FuelCardRepository repository;
    private final FuelCardService fuelCardService;
    private final FuelApprovalService fuelApprovalService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
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
    @PreAuthorize("hasRole('HR_ADMIN_MANAGER')")
    public ApprovalResponse approveFuel(@Valid @RequestBody FuelApprovalRequest request) {
        Approval approval = fuelApprovalService.decide(
                request.requestId(),
                request.approved(),
                request.usePersonalCard(),
                currentUser.get().getFullName(),
                request.comments());
        return ApprovalResponse.from(approval);
    }
}
