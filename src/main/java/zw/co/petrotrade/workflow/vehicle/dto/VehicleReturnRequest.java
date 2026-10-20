package zw.co.petrotrade.workflow.vehicle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// recorded as returned by the signed-in user
public record VehicleReturnRequest(
        @NotNull Long requestId,
        // litres actually drawn on the trip's fuel card; deducted from the card
        @NotNull @PositiveOrZero Double fuelUsedLitres,
        // odometer when the vehicle came back; the distance travelled is worked out from it
        @NotNull @PositiveOrZero Double odometerInKm) {
}
