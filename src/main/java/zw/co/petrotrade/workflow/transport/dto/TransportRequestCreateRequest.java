package zw.co.petrotrade.workflow.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.fuel.FuelType;
import zw.co.petrotrade.workflow.transport.DateRequiredException;
import zw.co.petrotrade.workflow.transport.TransportRequest;

import java.time.LocalDate;

// driverId defaults to the signed-in user; createdBy is always the signed-in user
public record TransportRequestCreateRequest(
        Long driverId,
        @NotNull LocalDate requiredFrom,
        @NotNull LocalDate requiredTo,
        @NotBlank String purpose,
        @NotBlank String destination,
        @NotBlank String startingPoint,
        @NotNull Double distanceKm,
        Long fuelTypeId,
        String requestedVehicleReg,
        Double tollFees,
        String signature,
        String notes) {

    public TransportRequest toEntity(Long driverId, String createdBy, FuelType fuelType) {
        TransportRequest request = new TransportRequest();
        request.setDriverId(driverId);
        LocalDate today = LocalDate.now();
        if (requiredFrom.isAfter(requiredTo)) {
            throw new DateRequiredException("The end date can't be before the start date.");
        }
        if (requiredFrom.isBefore(today)) {
            throw new DateRequiredException("The trip can't start in the past. Pick today or a later date.");
        }
        request.setRequiredFrom(requiredFrom);
        request.setRequiredTo(requiredTo);
        request.setPurpose(purpose);
        request.setDestination(destination);
        request.setStartingPoint(startingPoint);
        request.setDistanceKm(distanceKm);
        request.setFuelType(fuelType);
        request.setRequestedVehicleReg(requestedVehicleReg);
        request.setTollFees(tollFees);
        request.setCreatedBy(createdBy);
        request.setSignature(signature);
        request.setNotes(notes);
        return request;
    }
}
