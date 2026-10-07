package zw.co.petrotrade.workflow.fuel;

import zw.co.petrotrade.workflow.fuel.dto.FuelTypeRequest;

import java.util.List;

public interface FuelTypeService {
    FuelType create(FuelTypeRequest request);
    List<FuelType> findAll();
    FuelType findById(Long id);
    FuelType update(Long id, FuelTypeRequest request);
    void delete(Long id);
}
