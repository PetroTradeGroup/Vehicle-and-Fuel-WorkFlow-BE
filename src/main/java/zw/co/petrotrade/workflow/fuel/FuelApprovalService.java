package zw.co.petrotrade.workflow.fuel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalStatus;
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
    private final FuelCardRepository fuelCardRepository;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final ApprovalRepository approvalRepository;
    private final VehicleAllocationRepository vehicleAllocationRepository;

    public Approval decide(
            Long requestId,
            Long fuelCardId,
            boolean approved,
            String approver,
            String comments) {

        TransportRequest request = requestRepository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.FUEL_CALCULATED) {
            throw new IllegalStateException(
                    "Request " + requestId + " is not awaiting fuel approval (current status: "
                            + request.getStatus() + ")");
        }

        if (approved) {
            FuelCard card = fuelCardRepository.findById(fuelCardId).orElseThrow();

            if (card.getBalanceLitres() < request.getFuelRequiredLitres()) {
                throw new IllegalStateException(
                        "Fuel card " + fuelCardId + " balance (" + card.getBalanceLitres()
                                + "L) is insufficient for " + request.getFuelRequiredLitres() + "L");
            }

            card.setBalanceLitres(card.getBalanceLitres() - request.getFuelRequiredLitres());
            fuelCardRepository.save(card);

            VehicleAllocation allocation =
                    vehicleAllocationRepository.findByRequestId(requestId).orElseThrow();

            FuelTransaction transaction = new FuelTransaction();
            transaction.setRequestId(requestId);
            transaction.setFuelCardId(fuelCardId);
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

        Approval approval = new Approval();
        approval.setRequestId(requestId);
        approval.setApprover(approver);
        approval.setLevel(ApprovalLevel.FUEL);
        approval.setComments(comments);
        approval.setStatus(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        approval.setApprovalDate(LocalDateTime.now());

        return approvalRepository.save(approval);
    }
}
