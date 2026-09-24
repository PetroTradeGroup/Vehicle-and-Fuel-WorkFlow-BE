package zw.co.petrotrade.workflow.transport.dto;

import zw.co.petrotrade.workflow.fuel.FuelType;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;

import java.time.LocalDate;

public record TransportRequestResponse(
        Long id,
        Long driverId,
        String driverName,
        String department,
        String jobTitle,
        String licenceNumber,
        LocalDate requiredFrom,
        LocalDate requiredTo,
        String purpose,
        String destination,
        String startingPoint,
        Double distanceKm,
        Double fuelRequiredLitres,
        FuelType fuelType,
        String requestedVehicleReg,
        Double tollFees,
        String createdBy,
        String signature,
        String notes,
        RequestStatus status) {

    public static TransportRequestResponse from(TransportRequest request) {
        return new TransportRequestResponse(
                request.getId(),
                request.getDriverId(),
                request.getDriverName(),
                request.getDepartment(),
                request.getJobTitle(),
                request.getLicenceNumber(),
                request.getRequiredFrom(),
                request.getRequiredTo(),
                request.getPurpose(),
                request.getDestination(),
                request.getStartingPoint(),
                request.getDistanceKm(),
                request.getFuelRequiredLitres(),
                request.getFuelType(),
                request.getRequestedVehicleReg(),
                request.getTollFees(),
                request.getCreatedBy(),
                request.getSignature(),
                request.getNotes(),
                request.getStatus());
    }
}
