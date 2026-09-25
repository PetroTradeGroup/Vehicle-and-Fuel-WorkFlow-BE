package zw.co.petrotrade.workflow.station;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.fuel.CardHolderType;
import zw.co.petrotrade.workflow.fuel.FuelCard;
import zw.co.petrotrade.workflow.fuel.FuelCardService;
import zw.co.petrotrade.workflow.fuel.FuelTransaction;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.station.dto.StationTopUpRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StationTopUpService {

    private final StationTopUpRepository repository;
    private final StationRepository stationRepository;
    private final FuelCardService fuelCardService;
    private final FuelTransactionRepository fuelTransactionRepository;
    private final ApprovalRepository approvalRepository;

    public StationTopUp create(StationTopUpRequest request) {
        stationRepository.findById(request.stationId()).orElseThrow();
        // fail now rather than at approval if the station has nothing to credit
        fuelCardService.findActiveCard(CardHolderType.STATION, request.stationId());

        StationTopUp topUp = new StationTopUp();
        topUp.setStationId(request.stationId());
        topUp.setLitres(request.litres());
        topUp.setReason(request.reason());
        topUp.setRequestedBy(request.requestedBy());
        topUp.setRequestedAt(LocalDateTime.now());
        topUp.setStatus(TopUpStatus.PENDING_APPROVAL);
        return repository.save(topUp);
    }

    public List<StationTopUp> findAll() {
        return repository.findAll();
    }

    public StationTopUp findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public List<Approval> approvals(Long id) {
        findById(id);
        return approvalRepository.findBySubjectAndRequestIdOrderByApprovalDateAsc(ApprovalSubject.STATION_TOP_UP, id);
    }

    @Transactional
    public StationTopUp decide(Long id, boolean approved, String approver, String comments) {
        StationTopUp topUp = findById(id);

        if (topUp.getStatus() != TopUpStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Top-up " + id + " is not awaiting approval (current status: " + topUp.getStatus() + ")");
        }

        if (approved) {
            FuelCard card = fuelCardService.credit(
                    fuelCardService.findActiveCard(CardHolderType.STATION, topUp.getStationId()),
                    topUp.getLitres());

            FuelTransaction transaction = new FuelTransaction();
            transaction.setStationTopUpId(id);
            transaction.setFuelCardId(card.getId());
            transaction.setAllocatedLitres(topUp.getLitres());
            transaction.setConsumedLitres(0.0);
            transaction.setRemainingBalance(card.getBalanceLitres());
            transaction.setTransactionDate(LocalDateTime.now());
            fuelTransactionRepository.save(transaction);

            topUp.setStatus(TopUpStatus.COMPLETED);
        } else {
            topUp.setStatus(TopUpStatus.REJECTED);
        }

        approvalRepository.save(Approval.record(
                ApprovalSubject.STATION_TOP_UP, id, ApprovalLevel.HR_ADMIN, approved, approver, comments));

        return repository.save(topUp);
    }
}
