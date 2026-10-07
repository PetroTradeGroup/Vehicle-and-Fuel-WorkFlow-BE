package zw.co.petrotrade.workflow.fuel.dto;

import zw.co.petrotrade.workflow.fuel.FuelType;

public record FuelTypeResponse(
        Long id,
        String name,
        String code) {

    public static FuelTypeResponse from(FuelType fuelType) {
        return fuelType == null ? null : new FuelTypeResponse(fuelType.getId(), fuelType.getName(), fuelType.getCode());
    }
}
