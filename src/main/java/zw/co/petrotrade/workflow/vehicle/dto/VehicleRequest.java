package zw.co.petrotrade.workflow.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import zw.co.petrotrade.workflow.vehicle.VehicleStatus;

public record VehicleRequest(
        @NotBlank String registrationNumber,
        @NotBlank String make,
        @NotBlank String model,
        VehicleStatus status) {
}
