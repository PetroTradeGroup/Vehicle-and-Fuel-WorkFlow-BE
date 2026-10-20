package zw.co.petrotrade.workflow.vehicle.dto;

import zw.co.petrotrade.workflow.vehicle.ServiceStatus;
import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleStatus;

import java.time.LocalDate;

public record VehicleResponse(
        Long id,
        String registrationNumber,
        String make,
        String model,
        VehicleStatus status,
        Double odometerKm,
        Double lastServiceOdometerKm,
        LocalDate lastServiceDate,
        Double kmToService,
        ServiceStatus serviceStatus) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getStatus(),
                vehicle.getOdometerKm(),
                vehicle.getLastServiceOdometerKm(),
                vehicle.getLastServiceDate(),
                ServiceStatus.kmToService(vehicle),
                ServiceStatus.of(vehicle));
    }
}
