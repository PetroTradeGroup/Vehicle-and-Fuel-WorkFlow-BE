package zw.co.petrotrade.workflow.fuel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zw.co.petrotrade.workflow.fuel.FuelType;

public record FuelTypeRequest(
        @NotBlank String name,
        @NotNull String code
) {
    public FuelType toEntity() {
        FuelType fuelType = new FuelType();
        fuelType.setName(name);
        fuelType.setCode(code);
        return fuelType;

    }
}
