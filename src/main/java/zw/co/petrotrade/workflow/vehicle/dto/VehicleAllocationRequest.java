package zw.co.petrotrade.workflow.vehicle.dto;

import jakarta.validation.constraints.NotNull;

// recorded as allocated by the signed-in user
public record VehicleAllocationRequest(
        @NotNull Long requestId,
        @NotNull Long vehicleId) {
}
