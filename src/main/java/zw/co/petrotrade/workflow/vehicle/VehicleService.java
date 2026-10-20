package zw.co.petrotrade.workflow.vehicle;

import zw.co.petrotrade.workflow.vehicle.dto.VehicleRequest;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleServiceRequest;

import java.util.List;

public interface VehicleService
{
    Vehicle create(VehicleRequest request);
    List<Vehicle> findAll();
    Vehicle findById(Long id);
    Vehicle update(Long id, VehicleRequest request);
    Vehicle recordService(Long id, VehicleServiceRequest request);
    void delete(Long id);

}
