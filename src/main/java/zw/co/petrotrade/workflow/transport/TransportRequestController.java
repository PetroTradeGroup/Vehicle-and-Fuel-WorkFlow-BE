package zw.co.petrotrade.workflow.transport;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.fuel.FuelType;
import zw.co.petrotrade.workflow.fuel.FuelTypeService;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.transport.dto.ExtensionDecisionRequest;
import zw.co.petrotrade.workflow.transport.dto.ExtensionRequest;
import zw.co.petrotrade.workflow.transport.dto.FuelCalculationRequest;
import zw.co.petrotrade.workflow.transport.dto.RequestAuditResponse;
import zw.co.petrotrade.workflow.transport.dto.TransportRequestCreateRequest;
import zw.co.petrotrade.workflow.transport.dto.TransportRequestResponse;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationRepository;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class TransportRequestController {

    private final TransportRequestService service;
    private final ReturnDelayService returnDelays;
    private final TransportRequestRepository requestRepository;
    private final ApprovalRepository approvalRepository;
    private final VehicleAllocationRepository vehicleAllocationRepository;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final FuelTypeService fuelTypeService;
    private final CurrentUser currentUser;

    @PostMapping
    public TransportRequestResponse create(@Valid @RequestBody TransportRequestCreateRequest request) {
        User me = currentUser.get();
        Long driverId = request.driverId() == null ? me.getId() : request.driverId();
        if (!driverId.equals(me.getId())) {
            // raising a trip for someone else is an admin job
            currentUser.require(Role.VEHICLE_ADMIN, Role.SYSTEM_ADMIN);
        }
        FuelType fuelType = request.fuelTypeId() == null ? null : fuelTypeService.findById(request.fuelTypeId());
        return TransportRequestResponse.from(service.create(request.toEntity(driverId, me.getFullName(), fuelType)));
    }

    // both filters optional, e.g. ?status=PENDING_HOD for an approval inbox or ?driverId=3 for "my requests"
    @GetMapping
    public List<TransportRequestResponse> list(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long driverId) {
        TransportRequest probe = new TransportRequest();
        probe.setStatus(status);
        probe.setDriverId(driverId);
        if (!seesAll()) {
            User me = currentUser.get();
            if (currentUser.has(Role.HOD, Role.RETAIL_MANAGER) && me.getDepartment() != null) {
                probe.setDepartmentId(me.getDepartment().getId());
            } else {
                probe.setDriverId(me.getId());
            }
        }
        return requestRepository.findAll(Example.of(probe), Sort.by(Sort.Direction.DESC, "id"))
                .stream().map(TransportRequestResponse::from).toList();
    }

    @PostMapping("/{id}/calculate-fuel")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public TransportRequestResponse calculateFuel(@PathVariable Long id, @Valid @RequestBody(required = false) FuelCalculationRequest body) {
        return TransportRequestResponse.from(service.calculateFuel(id, body == null ? null : body.litres(), body == null ? null : body.reason()));
    }

    @PostMapping("/{id}/calculate-fuel/undo")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public TransportRequestResponse undoFuelCalculation(@PathVariable Long id) {
        return TransportRequestResponse.from(service.undoFuelCalculation(id));
    }

    // the driver (or a vehicle admin for them) asks to keep the vehicle longer, or explains why it's late
    @PostMapping("/{id}/extension")
    public TransportRequestResponse requestExtension(@PathVariable Long id, @Valid @RequestBody ExtensionRequest body) {
        TransportRequest request = requestRepository.findById(id).orElseThrow();
        if (request.getDriverId() == null || !request.getDriverId().equals(currentUser.get().getId())) {
            currentUser.require(Role.VEHICLE_ADMIN, Role.SYSTEM_ADMIN);
        }
        return TransportRequestResponse.from(returnDelays.requestExtension(id, body.returnDate(), body.reason()));
    }

    @PostMapping("/{id}/extension/decision")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public TransportRequestResponse decideExtension(@PathVariable Long id, @Valid @RequestBody ExtensionDecisionRequest body) {
        return TransportRequestResponse.from(returnDelays.decideExtension(id, body.approved(), currentUser.get().getFullName(), body.comments()));
    }

    @GetMapping("/{id}/audit")
    public RequestAuditResponse audit(@PathVariable Long id) {
        TransportRequest request = requestRepository.findById(id).orElseThrow();
        checkCanSee(request);
        RequestAudit audit = new RequestAudit(
                request,
                approvalRepository.findBySubjectAndRequestIdOrderByApprovalDateAsc(
                        ApprovalSubject.TRANSPORT_REQUEST, id),
                vehicleAllocationRepository.findByRequestId(id).orElse(null),
                fuelTransactionRepository.findFirstByRequestIdOrderByIdDesc(id).orElse(null));
        return RequestAuditResponse.from(audit);
    }

    // HR & Admin, vehicle admins and system admins see every request
    private boolean seesAll() {
        return currentUser.has(Role.HR_ADMIN_MANAGER, Role.VEHICLE_ADMIN, Role.SYSTEM_ADMIN);
    }

    // a head of department sees their department's requests; everyone else only their own
    private void checkCanSee(TransportRequest request) {
        if (seesAll()) return;
        User me = currentUser.get();
        if (request.getDriverId() != null && request.getDriverId().equals(me.getId())) return;
        if (currentUser.has(Role.HOD, Role.RETAIL_MANAGER) && me.getDepartment() != null
                && me.getDepartment().getId().equals(request.getDepartmentId())) return;
        throw new AccessDeniedException(currentUser.has(Role.HOD, Role.RETAIL_MANAGER)
                ? "You can only see requests from your own department."
                : "You can only see your own requests.");
    }
}
