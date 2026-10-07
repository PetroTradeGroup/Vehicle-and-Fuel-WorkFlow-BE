package zw.co.petrotrade.workflow.vehicle;

public interface VehicleAllocationService {
    VehicleAllocation allocate(Long requestId, Long vehicleId, String allocatedBy);
    VehicleAllocation returnVehicle(Long requestId, String returnedBy, double fuelUsedLitres, Double distanceTravelledKm);
}
