package zw.co.petrotrade.workflow.vehicle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

// recorded as returned by the signed-in user
public record VehicleReturnRequest(
        @NotNull Long requestId,
        // litres actually drawn on the trip's fuel card; deducted from the card
        @NotNull @PositiveOrZero Double fuelUsedLitres,
        // optional: the real distance, from the odometer
        @Positive Double distanceTravelledKm) {
}
