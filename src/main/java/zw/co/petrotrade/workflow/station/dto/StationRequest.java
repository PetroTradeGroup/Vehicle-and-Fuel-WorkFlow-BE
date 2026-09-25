package zw.co.petrotrade.workflow.station.dto;

import jakarta.validation.constraints.NotBlank;
import zw.co.petrotrade.workflow.station.Station;

public record StationRequest(
        @NotBlank String name,
        String location) {

    public Station toEntity() {
        Station station = new Station();
        station.setName(name);
        station.setLocation(location);
        return station;
    }
}
