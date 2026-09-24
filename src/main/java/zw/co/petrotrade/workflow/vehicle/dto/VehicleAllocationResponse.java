package zw.co.petrotrade.workflow.vehicle.dto;

import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;

import java.time.LocalDateTime;

public record VehicleAllocationResponse(
        Long id,
        Long requestId,
        Long vehicleId,
        String allocatedBy,
        LocalDateTime allocationDate) {

    public static VehicleAllocationResponse from(VehicleAllocation allocation) {
        return new VehicleAllocationResponse(
                allocation.getId(),
                allocation.getRequestId(),
                allocation.getVehicleId(),
                allocation.getAllocatedBy(),
                allocation.getAllocationDate());
    }
}
