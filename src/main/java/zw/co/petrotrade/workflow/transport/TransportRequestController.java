package zw.co.petrotrade.workflow.transport;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.transport.dto.RequestAuditResponse;
import zw.co.petrotrade.workflow.transport.dto.TransportRequestCreateRequest;
import zw.co.petrotrade.workflow.transport.dto.TransportRequestResponse;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationRepository;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class TransportRequestController {

    private final TransportRequestService service;
    private final TransportRequestRepository requestRepository;
    private final ApprovalRepository approvalRepository;
    private final VehicleAllocationRepository vehicleAllocationRepository;

    @PostMapping
    public TransportRequestResponse create(@Valid @RequestBody TransportRequestCreateRequest request) {
        return TransportRequestResponse.from(service.create(request.toEntity()));
    }

    @PostMapping("/{id}/calculate-fuel")
    public TransportRequestResponse calculateFuel(@PathVariable Long id) {
        return TransportRequestResponse.from(service.calculateFuel(id));
    }

    @GetMapping("/{id}/audit")
    public RequestAuditResponse audit(@PathVariable Long id) {
        TransportRequest request = requestRepository.findById(id).orElseThrow();
        RequestAudit audit = new RequestAudit(
                request,
                approvalRepository.findByRequestIdOrderByApprovalDateAsc(id),
                vehicleAllocationRepository.findByRequestId(id).orElse(null));
        return RequestAuditResponse.from(audit);
    }
}
