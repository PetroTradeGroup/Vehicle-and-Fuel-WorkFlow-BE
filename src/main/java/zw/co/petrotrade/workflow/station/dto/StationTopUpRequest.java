package zw.co.petrotrade.workflow.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import zw.co.petrotrade.workflow.station.TopUpPurpose;

// requested by the signed-in user
public record StationTopUpRequest(
        @NotNull Long stationId,
        @NotNull Long fuelTypeId,
        @NotNull @Positive Double litres,
        @NotNull TopUpPurpose purpose,
        // the generator's running hours now; required for the generator, ignored otherwise
        @PositiveOrZero Double generatorHours,
        @NotBlank String reason) {
}
