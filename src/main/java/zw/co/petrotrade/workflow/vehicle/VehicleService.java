package zw.co.petrotrade.workflow.vehicle;

import zw.co.petrotrade.workflow.vehicle.dto.VehicleRequest;

import java.util.List;

public interface VehicleService
{
    Vehicle create(VehicleRequest request);
    List<Vehicle> findAll();
    Vehicle findById(Long id);
    Vehicle update(Long id, VehicleRequest request);
    void delete(Long id);

}
