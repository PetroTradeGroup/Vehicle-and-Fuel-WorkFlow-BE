package zw.co.petrotrade.workflow.station.dto;

import zw.co.petrotrade.workflow.station.Station;

public record StationResponse(
        Long id,
        String name,
        String location) {

    public static StationResponse from(Station station) {
        return new StationResponse(station.getId(), station.getName(), station.getLocation());
    }
}
