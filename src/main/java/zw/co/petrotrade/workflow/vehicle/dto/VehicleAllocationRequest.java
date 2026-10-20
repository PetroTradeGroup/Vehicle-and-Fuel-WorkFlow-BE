package zw.co.petrotrade.workflow.vehicle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// recorded as allocated by the signed-in user
public record VehicleAllocationRequest(
        @NotNull Long requestId,
        @NotNull Long vehicleId,
        // odometer before the vehicle goes out; defaults to the reading it last came back with
        @PositiveOrZero Double odometerOutKm) {
}
