package zw.co.petrotrade.workflow.vehicle;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleAllocationRequest;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleAllocationResponse;

@RestController
@RequestMapping("/api/vehicle-allocations")
@RequiredArgsConstructor
public class VehicleAllocationController {

    private final VehicleAllocationService service;

    @PostMapping
    public VehicleAllocationResponse allocate(@Valid @RequestBody VehicleAllocationRequest request) {
        VehicleAllocation allocation = service.allocate(
                request.requestId(),
                request.vehicleId(),
                request.allocatedBy());
        return VehicleAllocationResponse.from(allocation);
    }
}
