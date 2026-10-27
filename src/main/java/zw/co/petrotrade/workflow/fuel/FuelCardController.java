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
import zw.co.petrotrade.workflow.user.Role;
import org.springframework.security.access.AccessDeniedException;

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
        boolean stations = seesStations();
        return repository.findAll().stream()
                .filter(c -> stations || c.getHolderType() != CardHolderType.STATION)
                .map(FuelCardResponse::from).toList();
    }

    @GetMapping("/{id}")
    public FuelCardResponse get(@PathVariable Long id) {
        FuelCard card = repository.findById(id).orElseThrow();
        if (card.getHolderType() == CardHolderType.STATION && !seesStations()) {
            throw new AccessDeniedException("You don't have permission to see station cards.");
        }
        return FuelCardResponse.from(card);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public FuelCardResponse update(@PathVariable Long id, @Valid @RequestBody FuelCardRequest request) {
        return FuelCardResponse.from(fuelCardService.update(id, request));
    }

    // vehicle admins see nothing about stations, unless they also oversee them
    private boolean seesStations() {
        return !currentUser.has(Role.VEHICLE_ADMIN) || currentUser.has(Role.HR_ADMIN_MANAGER, Role.SYSTEM_ADMIN);
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
