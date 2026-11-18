package zw.co.petrotrade.workflow.station.dto;

import zw.co.petrotrade.workflow.fuel.dto.FuelTypeResponse;
import zw.co.petrotrade.workflow.station.StationTopUp;
import zw.co.petrotrade.workflow.station.TopUpPurpose;
import zw.co.petrotrade.workflow.station.TopUpStatus;

import java.time.LocalDateTime;

public record StationTopUpResponse(
        Long id,
        Long stationId,
        Double litres,
        FuelTypeResponse fuelType,
        TopUpPurpose purpose,
        Double generatorHours,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt,
        TopUpStatus status) {

    public static StationTopUpResponse from(StationTopUp topUp) {
        return new StationTopUpResponse(
                topUp.getId(),
                topUp.getStationId(),
                topUp.getLitres(),
                FuelTypeResponse.from(topUp.getFuelType()),
                topUp.getPurpose(),
                topUp.getGeneratorHours(),
                topUp.getReason(),
                topUp.getRequestedBy(),
                topUp.getRequestedAt(),
                topUp.getStatus());
    }
}
