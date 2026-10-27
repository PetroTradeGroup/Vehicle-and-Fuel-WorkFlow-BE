package zw.co.petrotrade.workflow.fuel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;
import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleStatus;

import java.time.LocalDateTime;

// HR & Admin's one decision on a trip, made last: approving signs off the trip and loads its fuel
@Service
@RequiredArgsConstructor
public class FuelApprovalService {

    private final TransportRequestRepository requestRepository;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final ApprovalRepository approvalRepository;
    private final VehicleAllocationRepository vehicleAllocationRepository;
    private final FuelCardService fuelCardService;
    private final FuelCardRepository fuelCardRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationService notifications;

    @Transactional
    public Approval decide(
            Long requestId,
            boolean approved,
            boolean usePersonalCard,
            String approver,
            String comments) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.FUEL_CALCULATED) {
            throw new IllegalStateException("This request isn't waiting for HR & Admin approval. "
                    + request.getStatus().alreadyMovedOn());
        }

        VehicleAllocation allocation =
                vehicleAllocationRepository.findByRequestId(requestId).orElseThrow();

        boolean loadsFuel = request.getFuelRequiredLitres() != null && request.getFuelRequiredLitres() > 0;
        if (approved && !loadsFuel) {
            // the vehicle admin found enough in the tank: the trip is signed off with nothing loaded
            request.setStatus(RequestStatus.FUEL_APPROVED);
        } else if (approved) {
            // the card is picked now but only credited once IT has loaded the fuel onto it
            FuelCard card = resolveCard(request, allocation, usePersonalCard);

            FuelTransaction transaction = new FuelTransaction();
            transaction.setRequestId(requestId);
            transaction.setFuelCardId(card.getId());
            transaction.setVehicleId(allocation.getVehicleId());
            transaction.setDriverId(request.getDriverId());
            transaction.setAllocatedLitres(request.getFuelRequiredLitres());
            transaction.setConsumedLitres(0.0);
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setLoaded(false);
            fuelTransactionRepository.save(transaction);

            // the trip is still ahead; the vehicle is released when it is returned
            request.setStatus(RequestStatus.FUEL_APPROVED);
        } else {
            request.setStatus(RequestStatus.REJECTED_HR_ADMIN);

            // a rejected trip never leaves, so the vehicle is free for the next request
            Vehicle vehicle = vehicleRepository.findById(allocation.getVehicleId()).orElseThrow();
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);
        }

        requestRepository.save(request);
        notifications.notifyUser(request.getDriverId(), "Request #" + requestId
                        + (!approved ? " rejected by HR & Admin" : loadsFuel ? " approved: fuel is being loaded" : " approved: you can go"),
                approved
                        ? "HR & Admin approved your trip to " + request.getDestination() + ". "
                                + (loadsFuel ? "IT will load " + request.getFuelRequiredLitres() + " litres of fuel; you'll be told when it's on the card.\n"
                                        : "No fuel was loaded: the vehicle has enough.\n")
                        : "HR & Admin rejected your trip to " + request.getDestination() + ".\n"
                                + (comments == null || comments.isBlank() ? "" : "Comment from " + approver + ": " + comments.trim() + "\n"),
                NotificationService.requestLink(requestId));

        return approvalRepository.save(Approval.record(
                ApprovalSubject.TRANSPORT_REQUEST, requestId, ApprovalLevel.HR_ADMIN, approved, approver, comments));
    }

    // IT has put the approved fuel on the physical card: only now does the card's balance go up
    @Transactional
    public FuelTransaction markLoaded(Long transactionId, String loadedBy) {
        FuelTransaction transaction = fuelTransactionRepository.findById(transactionId).orElseThrow();
        if (transaction.getRequestId() == null || !Boolean.FALSE.equals(transaction.getLoaded())) {
            throw new IllegalStateException("This fuel has already been loaded. Refresh to see the latest.");
        }
        FuelCard card = fuelCardRepository.findById(transaction.getFuelCardId()).orElseThrow();
        if (!Boolean.TRUE.equals(card.getActive())) {
            throw new IllegalStateException("Fuel card " + card.getCardNumber() + " is switched off, so fuel can't be loaded onto it.");
        }
        card = fuelCardService.credit(card, transaction.getAllocatedLitres());

        transaction.setLoaded(true);
        transaction.setLoadedBy(loadedBy);
        transaction.setLoadedAt(LocalDateTime.now());
        transaction.setRemainingBalance(card.getBalanceLitres());
        fuelTransactionRepository.save(transaction);

        TransportRequest request = requestRepository.findById(transaction.getRequestId()).orElseThrow();
        notifications.notifyUser(request.getDriverId(), "Request #" + request.getId() + ": fuel loaded, you can go",
                transaction.getAllocatedLitres() + " litres were loaded onto fuel card " + card.getCardNumber()
                        + " for your trip to " + request.getDestination() + ".\n",
                NotificationService.requestLink(request.getId()));
        return transaction;
    }

    private FuelCard resolveCard(TransportRequest request, VehicleAllocation allocation, boolean usePersonalCard) {
        return usePersonalCard
                ? fuelCardService.findActiveCard(CardHolderType.USER, request.getDriverId())
                : fuelCardService.findActiveCard(CardHolderType.VEHICLE, allocation.getVehicleId());
    }
}
