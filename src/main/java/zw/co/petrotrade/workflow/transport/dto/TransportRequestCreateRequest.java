package zw.co.petrotrade.workflow.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.fuel.FuelType;
import zw.co.petrotrade.workflow.transport.DateRequiredException;
import zw.co.petrotrade.workflow.transport.TransportRequest;

import java.time.LocalDate;

public record TransportRequestCreateRequest(
        @NotNull Long driverId,
        @NotNull LocalDate requiredFrom,
        @NotNull LocalDate requiredTo,
        @NotBlank String purpose,
        @NotBlank String destination,
        @NotBlank String startingPoint,
        @NotNull Double distanceKm,
        FuelType fuelType,
        String requestedVehicleReg,
        Double tollFees,
        @NotBlank String createdBy,
        String signature,
        String notes) {

    public TransportRequest toEntity() {
        TransportRequest request = new TransportRequest();
        request.setDriverId(driverId);
        LocalDate today = LocalDate.now();
        if (requiredFrom.isAfter(requiredTo)) {
            throw new DateRequiredException(
                    "Required-from date " + requiredFrom + " must not be after required-to date " + requiredTo);
        }
        if (requiredFrom.isBefore(today)) {
            throw new DateRequiredException(
                    "Required-from date " + requiredFrom + " must not be in the past");
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
