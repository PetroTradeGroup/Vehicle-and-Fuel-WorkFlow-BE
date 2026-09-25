package zw.co.petrotrade.workflow.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StationTopUpRequest(
        @NotNull Long stationId,
        @NotNull @Positive Double litres,
        @NotBlank String reason,
        @NotBlank String requestedBy) {
}
