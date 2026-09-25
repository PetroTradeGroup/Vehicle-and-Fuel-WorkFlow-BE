package zw.co.petrotrade.workflow.station;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.approval.dto.ApprovalResponse;
import zw.co.petrotrade.workflow.station.dto.StationTopUpRequest;
import zw.co.petrotrade.workflow.station.dto.StationTopUpResponse;
import zw.co.petrotrade.workflow.station.dto.TopUpDecisionRequest;

import java.util.List;

@RestController
@RequestMapping("/api/station-top-ups")
@RequiredArgsConstructor
public class StationTopUpController {

    private final StationTopUpService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationTopUpResponse create(@Valid @RequestBody StationTopUpRequest request) {
        return StationTopUpResponse.from(service.create(request));
    }

    @GetMapping
    public List<StationTopUpResponse> list() {
        return service.findAll().stream().map(StationTopUpResponse::from).toList();
    }

    @GetMapping("/{id}")
    public StationTopUpResponse get(@PathVariable Long id) {
        return StationTopUpResponse.from(service.findById(id));
    }

    @GetMapping("/{id}/approvals")
    public List<ApprovalResponse> approvals(@PathVariable Long id) {
        return service.approvals(id).stream().map(ApprovalResponse::from).toList();
    }

    @PostMapping("/{id}/decision")
    public StationTopUpResponse decide(@PathVariable Long id, @Valid @RequestBody TopUpDecisionRequest request) {
        return StationTopUpResponse.from(
                service.decide(id, request.approved(), request.approver(), request.comments()));
    }
}
