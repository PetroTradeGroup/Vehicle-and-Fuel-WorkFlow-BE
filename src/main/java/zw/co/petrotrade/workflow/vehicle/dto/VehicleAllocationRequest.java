package zw.co.petrotrade.workflow.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleAllocationRequest(
        @NotNull Long requestId,
        @NotNull Long vehicleId,
        @NotBlank String allocatedBy) {
}
