package zw.co.petrotrade.workflow.vehicle.dto;

import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleStatus;

public record VehicleResponse(
        Long id,
        String registrationNumber,
        String make,
        String model,
        VehicleStatus status) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getStatus());
    }
}
