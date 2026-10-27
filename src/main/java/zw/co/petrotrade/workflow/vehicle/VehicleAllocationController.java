package zw.co.petrotrade.workflow.vehicle;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.transport.dto.TransportRequestResponse;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleAllocationRequest;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleAllocationResponse;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleReturnRequest;

@RestController
@RequestMapping("/api/vehicle-allocations")
@RequiredArgsConstructor
public class VehicleAllocationController {

    private final VehicleAllocationServiceImp service;
    private final CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public VehicleAllocationResponse allocate(@Valid @RequestBody VehicleAllocationRequest request) {
        VehicleAllocation allocation = service.allocate(
                request.requestId(),
                request.vehicleId(),
                currentUser.get().getFullName(),
                request.odometerOutKm());
        return VehicleAllocationResponse.from(allocation);
    }

    // back to "waiting for a vehicle", so a wrong vehicle can be swapped before HR & Admin approve
    @PostMapping("/{requestId}/undo")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public TransportRequestResponse undo(@PathVariable Long requestId) {
        return TransportRequestResponse.from(service.undoAllocation(requestId));
    }

    @PostMapping("/return")
    @PreAuthorize("hasRole('VEHICLE_ADMIN')")
    public VehicleAllocationResponse returnVehicle(@Valid @RequestBody VehicleReturnRequest request) {
        return VehicleAllocationResponse.from(service.returnVehicle(
                request.requestId(), currentUser.get().getFullName(), request.fuelUsedLitres(), request.odometerInKm()));
    }
}
