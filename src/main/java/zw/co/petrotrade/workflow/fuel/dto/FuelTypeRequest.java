package zw.co.petrotrade.workflow.fuel.dto;

import jakarta.validation.constraints.NotBlank;

public record FuelTypeRequest(
        @NotBlank String name,
        @NotBlank String code) {
}
