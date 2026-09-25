package zw.co.petrotrade.workflow.fuel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FuelApprovalService {

    private final TransportRequestRepository requestRepository;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final ApprovalRepository approvalRepository;
    private final VehicleAllocationRepository vehicleAllocationRepository;
    private final FuelCardService fuelCardService;

    @Transactional
    public Approval decide(
            Long requestId,
            boolean approved,
            boolean usePersonalCard,
            String approver,
            String comments) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.FUEL_CALCULATED) {
            throw new IllegalStateException(
                    "Request " + requestId + " is not awaiting fuel approval (current status: "
                            + request.getStatus() + ")");
        }

        if (approved) {
            VehicleAllocation allocation =
                    vehicleAllocationRepository.findByRequestId(requestId).orElseThrow();

            FuelCard card = fuelCardService.credit(
                    resolveCard(request, allocation, usePersonalCard), request.getFuelRequiredLitres());

            FuelTransaction transaction = new FuelTransaction();
            transaction.setRequestId(requestId);
            transaction.setFuelCardId(card.getId());
            transaction.setVehicleId(allocation.getVehicleId());
            transaction.setDriverId(request.getDriverId());
            transaction.setAllocatedLitres(request.getFuelRequiredLitres());
            transaction.setConsumedLitres(0.0);
            transaction.setRemainingBalance(card.getBalanceLitres());
            transaction.setTransactionDate(LocalDateTime.now());
            fuelTransactionRepository.save(transaction);

            request.setStatus(RequestStatus.COMPLETED);
        } else {
            request.setStatus(RequestStatus.FUEL_REJECTED);
        }

        requestRepository.save(request);

        return approvalRepository.save(Approval.record(
                ApprovalSubject.TRANSPORT_REQUEST, requestId, ApprovalLevel.FUEL, approved, approver, comments));
    }

    private FuelCard resolveCard(TransportRequest request, VehicleAllocation allocation, boolean usePersonalCard) {
        return usePersonalCard
                ? fuelCardService.findActiveCard(CardHolderType.USER, request.getDriverId())
                : fuelCardService.findActiveCard(CardHolderType.VEHICLE, allocation.getVehicleId());
    }
}
