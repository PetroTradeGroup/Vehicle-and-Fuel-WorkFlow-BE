package zw.co.petrotrade.workflow.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import zw.co.petrotrade.workflow.vehicle.VehicleStatus;

public record VehicleRequest(
        @NotBlank String registrationNumber,
        @NotBlank String make,
        @NotBlank String model,
        VehicleStatus status,
        // optional: the current odometer reading, so the first trip starts from it
        @PositiveOrZero Double odometerKm,
        // optional: the odometer at the last service, for vehicles added mid-way through a service interval
        @PositiveOrZero Double lastServiceOdometerKm) {
}
