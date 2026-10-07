package zw.co.petrotrade.workflow.vehicle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.petrotrade.workflow.fuel.FuelCard;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.fuel.FuelTransaction;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VehicleAllocationServiceImp implements VehicleAllocationService {

    private final VehicleAllocationRepository allocationRepository;
    private final VehicleRepository vehicleRepository;
    private final TransportRequestRepository requestRepository;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final FuelCardRepository fuelCardRepository;

    @Transactional
    public VehicleAllocation allocate(Long requestId, Long vehicleId, String allocatedBy) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.APPROVED_HR_ADMIN) {
            throw new IllegalStateException("A vehicle can only be allocated after department head and HR & Admin approval. "
                    + request.getStatus().alreadyMovedOn());
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow();

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber()
                    + " isn't available any more. Pick another one.");
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

    @Transactional
    public VehicleAllocation returnVehicle(Long requestId, String returnedBy, double fuelUsedLitres, Double distanceTravelledKm) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.FUEL_APPROVED) {
            throw new IllegalStateException("Only a vehicle that's out on a trip can be returned. "
                    + request.getStatus().alreadyMovedOn());
        }

        VehicleAllocation allocation = allocationRepository.findByRequestId(requestId).orElseThrow();

        // the fuel drawn on the trip comes off the card it was loaded onto
        FuelTransaction transaction = fuelTransactionRepository.findFirstByRequestIdOrderByIdDesc(requestId)
                .orElseThrow(() -> new IllegalStateException("No fuel was issued for this trip, so there's nothing to record usage against."));
        FuelCard card = fuelCardRepository.findById(transaction.getFuelCardId()).orElseThrow();
        if (fuelUsedLitres > card.getBalanceLitres()) {
            throw new IllegalArgumentException(String.format(
                    "%.1f L is more than fuel card %s holds (%.1f L). Check the litres used.",
                    fuelUsedLitres, card.getCardNumber(), card.getBalanceLitres()));
        }
        card.setBalanceLitres(card.getBalanceLitres() - fuelUsedLitres);
        fuelCardRepository.save(card);
        transaction.setConsumedLitres(fuelUsedLitres);
        transaction.setRemainingBalance(card.getBalanceLitres());
        fuelTransactionRepository.save(transaction);

        Vehicle vehicle = vehicleRepository.findById(allocation.getVehicleId()).orElseThrow();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);

        allocation.setReturnedBy(returnedBy);
        allocation.setReturnDate(LocalDateTime.now());
        allocation.setFuelUsedLitres(fuelUsedLitres);
        allocation.setDistanceTravelledKm(distanceTravelledKm);

        request.setStatus(RequestStatus.COMPLETED);
        requestRepository.save(request);

        return allocationRepository.save(allocation);
    }
}
