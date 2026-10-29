package zw.co.petrotrade.workflow.vehicle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.petrotrade.workflow.fuel.FuelCard;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.fuel.FuelTransaction;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;
import zw.co.petrotrade.workflow.transport.TransportRequestService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VehicleAllocationServiceImp implements VehicleAllocationService {

    private final VehicleAllocationRepository allocationRepository;
    private final VehicleRepository vehicleRepository;
    private final TransportRequestRepository requestRepository;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final FuelCardRepository fuelCardRepository;
    private final NotificationService notifications;

    @Transactional
    public VehicleAllocation allocate(Long requestId, Long vehicleId, String allocatedBy, Double odometerOutKm) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        // APPROVED_HR_ADMIN: requests HR & Admin approved before approval moved to the end of the workflow
        if (request.getStatus() != RequestStatus.APPROVED_HOD && request.getStatus() != RequestStatus.APPROVED_HR_ADMIN) {
            throw new IllegalStateException("A vehicle can only be allocated after department head approval. "
                    + request.getStatus().alreadyMovedOn());
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow();

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new IllegalStateException("Vehicle " + vehicle.getRegistrationNumber()
                    + " isn't available any more. Pick another one.");
        }

        // the reading it last came back with is where this trip starts, unless the admin read a different one
        Double odometerOut = odometerOutKm != null ? odometerOutKm : vehicle.getOdometerKm();
        if (odometerOut == null) {
            throw new IllegalArgumentException("Vehicle " + vehicle.getRegistrationNumber()
                    + " has no odometer reading yet. Enter the reading before it goes out.");
        }
        if (vehicle.getOdometerKm() != null && odometerOut < vehicle.getOdometerKm()) {
            throw new IllegalArgumentException(String.format(
                    "The odometer can't read less than %.0f km, the reading %s last came back with.",
                    vehicle.getOdometerKm(), vehicle.getRegistrationNumber()));
        }

        VehicleAllocation allocation = new VehicleAllocation();
        allocation.setOdometerBeforeKm(vehicle.getOdometerKm());

        vehicle.setStatus(VehicleStatus.ALLOCATED);
        vehicle.setOdometerKm(odometerOut);
        vehicleRepository.save(vehicle);

        allocation.setRequestId(requestId);
        allocation.setVehicleId(vehicleId);
        allocation.setAllocatedBy(allocatedBy);
        allocation.setAllocationDate(LocalDateTime.now());
        allocation.setOdometerOutKm(odometerOut);

        // the trip runs on what this vehicle takes; vehicles with no fuel type set leave the request's as it was
        if (vehicle.getFuelType() != null) {
            request.setFuelType(vehicle.getFuelType());
        }
        request.setStatus(RequestStatus.VEHICLE_ALLOCATED);
        requestRepository.save(request);
        notifications.notifyUser(request.getDriverId(), "Vehicle allocated - Request #" + requestId,
                vehicle.getRegistrationNumber() + " (" + vehicle.getMake() + " " + vehicle.getModel() + ") is allocated to your trip to "
                        + request.getDestination() + ". It goes out once its fuel is calculated and HR & Admin approve the trip.\n",
                NotificationService.requestLink(requestId));

        return allocationRepository.save(allocation);
    }

    // the vehicle admin picked the wrong vehicle: free it and send the request back to allocation.
    // Works until HR & Admin approve; any fuel calculated for it is undone too.
    @Transactional
    public TransportRequest undoAllocation(Long requestId) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.VEHICLE_ALLOCATED && request.getStatus() != RequestStatus.FUEL_CALCULATED) {
            throw new IllegalStateException("The vehicle can only be changed before HR & Admin approve the trip. "
                    + request.getStatus().alreadyMovedOn());
        }

        VehicleAllocation allocation = allocationRepository.findByRequestId(requestId).orElseThrow();
        Vehicle vehicle = vehicleRepository.findById(allocation.getVehicleId()).orElseThrow();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        // allocations from before this was recorded keep the reading they went out with
        if (allocation.getOdometerBeforeKm() != null) {
            vehicle.setOdometerKm(allocation.getOdometerBeforeKm());
        }
        vehicleRepository.save(vehicle);
        allocationRepository.delete(allocation);

        TransportRequestService.clearFuelCalculation(request);
        // the fuel type came from that vehicle; the next one brings its own
        if (vehicle.getFuelType() != null) {
            request.setFuelType(null);
        }
        request.setStatus(RequestStatus.APPROVED_HOD);
        notifications.notifyUser(request.getDriverId(), "Vehicle changed - Request #" + requestId,
                vehicle.getRegistrationNumber() + " is no longer allocated to your trip to " + request.getDestination()
                        + ". The vehicle admin is choosing another vehicle.\n",
                NotificationService.requestLink(requestId));
        return requestRepository.save(request);
    }

    @Transactional
    public VehicleAllocation returnVehicle(Long requestId, String returnedBy, double fuelUsedLitres, double odometerInKm) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.FUEL_APPROVED) {
            throw new IllegalStateException("Only a vehicle that's out on a trip can be returned. "
                    + request.getStatus().alreadyMovedOn());
        }

        VehicleAllocation allocation = allocationRepository.findByRequestId(requestId).orElseThrow();

        // trips allocated before odometers were recorded have no starting reading, so no distance
        Double odometerOut = allocation.getOdometerOutKm();
        if (odometerOut != null && odometerInKm < odometerOut) {
            throw new IllegalArgumentException(String.format(
                    "The odometer can't read less than when the vehicle went out (%.0f km). Check the reading.", odometerOut));
        }

        // the fuel drawn on the trip comes off the card it was loaded onto; a trip that went out
        // on the fuel already in the tank had nothing loaded, so only the usage is recorded
        FuelTransaction transaction = fuelTransactionRepository.findFirstByRequestIdOrderByIdDesc(requestId).orElse(null);
        if (transaction != null && Boolean.FALSE.equals(transaction.getLoaded())) {
            throw new IllegalStateException("IT hasn't loaded this trip's fuel yet, so nothing can come off the card. "
                    + "Ask IT to load it on the Fuel Loading page first.");
        }
        if (transaction != null) {
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
        }

        Vehicle vehicle = vehicleRepository.findById(allocation.getVehicleId()).orElseThrow();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        // the next trip starts from this reading
        vehicle.setOdometerKm(odometerInKm);
        vehicleRepository.save(vehicle);

        allocation.setReturnedBy(returnedBy);
        allocation.setReturnDate(LocalDateTime.now());
        allocation.setFuelUsedLitres(fuelUsedLitres);
        allocation.setOdometerInKm(odometerInKm);
        allocation.setDistanceTravelledKm(odometerOut == null ? null : odometerInKm - odometerOut);

        request.setStatus(RequestStatus.COMPLETED);
        requestRepository.save(request);
        notifications.notifyUser(request.getDriverId(), "Trip completed - Request #" + requestId,
                vehicle.getRegistrationNumber() + " was returned at " + Math.round(odometerInKm) + " km. Your trip to "
                        + request.getDestination() + " is closed.\n",
                NotificationService.requestLink(requestId));

        return allocationRepository.save(allocation);
    }
}
