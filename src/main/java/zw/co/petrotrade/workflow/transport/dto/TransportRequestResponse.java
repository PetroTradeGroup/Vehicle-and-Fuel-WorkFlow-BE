package zw.co.petrotrade.workflow.transport.dto;

import zw.co.petrotrade.workflow.fuel.dto.FuelTypeResponse;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.ReturnDelayService;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TripType;

import java.time.LocalDate;

public record TransportRequestResponse(
        Long id,
        Long driverId,
        String driverName,
        String department,
        Long departmentId,
        String jobTitle,
        String licenceNumber,
        LocalDate requiredFrom,
        LocalDate requiredTo,
        String purpose,
        String destination,
        String startingPoint,
        Double distanceKm,
        TripType tripType,
        Double fuelRequiredLitres,
        FuelTypeResponse fuelType,
        String requestedVehicleReg,
        Double tollFees,
        String createdBy,
        String signature,
        String notes,
        RequestStatus status,
        // out on the trip past its return date
        boolean overdue,
        LocalDate extensionRequestedTo,
        String delayReason) {

    public static TransportRequestResponse from(TransportRequest request) {
        return new TransportRequestResponse(
                request.getId(),
                request.getDriverId(),
                request.getDriverName(),
                request.getDepartment(),
                request.getDepartmentId(),
                request.getJobTitle(),
                request.getLicenceNumber(),
                request.getRequiredFrom(),
                request.getRequiredTo(),
                request.getPurpose(),
                request.getDestination(),
                request.getStartingPoint(),
                request.getDistanceKm(),
                TripType.of(request.getDistanceKm()),
                request.getFuelRequiredLitres(),
                FuelTypeResponse.from(request.getFuelType()),
                request.getRequestedVehicleReg(),
                request.getTollFees(),
                request.getCreatedBy(),
                request.getSignature(),
                request.getNotes(),
                request.getStatus(),
                ReturnDelayService.overdue(request),
                request.getExtensionRequestedTo(),
                request.getDelayReason());
    }
}
