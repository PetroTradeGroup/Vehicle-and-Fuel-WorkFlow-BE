package zw.co.petrotrade.workflow.vehicle;

public interface VehicleAllocationService {
    VehicleAllocation allocate(Long requestId, Long vehicleId, String allocatedBy, Double odometerOutKm);
    VehicleAllocation returnVehicle(Long requestId, String returnedBy, double fuelUsedLitres, double odometerInKm);
}
