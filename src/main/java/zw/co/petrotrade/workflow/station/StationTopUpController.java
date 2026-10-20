package zw.co.petrotrade.workflow.station;

import org.springframework.security.access.AccessDeniedException;
import zw.co.petrotrade.workflow.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.approval.dto.ApprovalResponse;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.station.dto.StationTopUpRequest;
import zw.co.petrotrade.workflow.station.dto.StationTopUpResponse;
import zw.co.petrotrade.workflow.station.dto.TopUpDecisionRequest;

import java.util.List;

// A station admin only ever sees and requests top-ups for their own station
@RestController
@RequestMapping("/api/station-top-ups")
@RequiredArgsConstructor
public class StationTopUpController {

    private final StationTopUpService service;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationTopUpResponse create(@Valid @RequestBody StationTopUpRequest request) {
        Long own = currentUser.stationScope();
        if (own != null && !own.equals(request.stationId())) {
            throw new AccessDeniedException("You can only request top-ups for your own station.");
        }
        return StationTopUpResponse.from(service.create(request, currentUser.get().getFullName()));
    }

    @GetMapping
    public List<StationTopUpResponse> list() {
        Long own = currentUser.stationScope();
        List<StationTopUp> topUps = own == null ? service.findAll() : service.findByStation(own);
        return topUps.stream().map(StationTopUpResponse::from).toList();
    }

    @GetMapping("/{id}")
    public StationTopUpResponse get(@PathVariable Long id) {
        return StationTopUpResponse.from(visible(id));
    }

    @GetMapping("/{id}/approvals")
    public List<ApprovalResponse> approvals(@PathVariable Long id) {
        visible(id);
        return service.approvals(id).stream().map(ApprovalResponse::from).toList();
    }

    @PostMapping("/{id}/decision")
    public StationTopUpResponse decide(@PathVariable Long id, @Valid @RequestBody TopUpDecisionRequest request) {
        // a regional rep or the retail manager first, then HR & Admin
        TopUpStatus stage = service.findById(id).getStatus();
        if (stage == TopUpStatus.PENDING_FIRST_APPROVAL) {
            currentUser.require(Role.REGIONAL_REP, Role.RETAIL_MANAGER);
        } else {
            currentUser.require(Role.HR_ADMIN_MANAGER);
        }
        return StationTopUpResponse.from(
                service.decide(id, stage, request.approved(), currentUser.get().getFullName(), request.comments()));
    }

    private StationTopUp visible(Long id) {
        StationTopUp topUp = service.findById(id);
        Long own = currentUser.stationScope();
        if (own != null && !own.equals(topUp.getStationId())) {
            throw new AccessDeniedException("You can only see your own station's top-ups.");
        }
        return topUp;
    }
}
