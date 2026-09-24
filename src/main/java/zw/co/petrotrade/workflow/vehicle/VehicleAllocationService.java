package zw.co.petrotrade.workflow.vehicle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VehicleAllocationService {

    private final VehicleAllocationRepository allocationRepository;
    private final VehicleRepository vehicleRepository;
    private final TransportRequestRepository requestRepository;

    public VehicleAllocation allocate(Long requestId, Long vehicleId, String allocatedBy) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.APPROVED_ADMIN) {
            throw new IllegalStateException(
                    "Request " + requestId + " has not completed HOD/HR/Admin approval (current status: "
                            + request.getStatus() + ")");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow();

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Vehicle " + vehicleId + " is not available");
        }

        vehicle.setStatus(VehicleStatus.ALLOCATED);
        vehicleRepository.save(vehicle);

        VehicleAllocation allocation = new VehicleAllocation();
        allocation.setRequestId(requestId);
        allocation.setVehicleId(vehicleId);
        allocation.setAllocatedBy(allocatedBy);
        allocation.setAllocationDate(LocalDateTime.now());

        request.setStatus(RequestStatus.VEHICLE_ALLOCATED);
        requestRepository.save(request);

        return allocationRepository.save(allocation);
    }
}
