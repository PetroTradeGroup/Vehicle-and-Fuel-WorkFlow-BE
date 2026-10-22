package zw.co.petrotrade.workflow.transport.dto;

import jakarta.validation.constraints.PositiveOrZero;

// litres: what the vehicle admin wants loaded on a local trip (0 when the tank has enough), left out for the calculated amount;
// reason: why, when it's more than calculated
public record FuelCalculationRequest(@PositiveOrZero Double litres, String reason) {
}
