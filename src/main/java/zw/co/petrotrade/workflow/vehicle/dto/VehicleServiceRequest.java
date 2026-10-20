package zw.co.petrotrade.workflow.vehicle.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

// both optional: the service is recorded at the vehicle's current odometer, today
public record VehicleServiceRequest(
        @PositiveOrZero Double odometerKm,
        @PastOrPresent LocalDate serviceDate) {
}
