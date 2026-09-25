package zw.co.petrotrade.workflow.station.dto;

import zw.co.petrotrade.workflow.station.StationTopUp;
import zw.co.petrotrade.workflow.station.TopUpStatus;

import java.time.LocalDateTime;

public record StationTopUpResponse(
        Long id,
        Long stationId,
        Double litres,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt,
        TopUpStatus status) {

    public static StationTopUpResponse from(StationTopUp topUp) {
        return new StationTopUpResponse(
                topUp.getId(),
                topUp.getStationId(),
                topUp.getLitres(),
                topUp.getReason(),
                topUp.getRequestedBy(),
                topUp.getRequestedAt(),
                topUp.getStatus());
    }
}
