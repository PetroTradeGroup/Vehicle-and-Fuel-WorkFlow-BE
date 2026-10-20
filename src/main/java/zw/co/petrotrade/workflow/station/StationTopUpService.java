package zw.co.petrotrade.workflow.station;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.station.dto.StationTopUpRequest;
import zw.co.petrotrade.workflow.user.Role;

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
    private final NotificationService notifications;

    public StationTopUp create(StationTopUpRequest request, String requestedBy) {
        Station station = stationRepository.findById(request.stationId()).orElseThrow();
        // fail now rather than at approval if the station has nothing to credit
        fuelCardService.findActiveCard(CardHolderType.STATION, request.stationId());

        StationTopUp topUp = new StationTopUp();
        topUp.setStationId(request.stationId());
        topUp.setLitres(request.litres());
        topUp.setReason(request.reason());
        topUp.setRequestedBy(requestedBy);
        topUp.setRequestedAt(LocalDateTime.now());
        topUp.setStatus(TopUpStatus.PENDING_FIRST_APPROVAL);
        StationTopUp saved = repository.save(topUp);
        notifications.notifyRoles(List.of(Role.REGIONAL_REP, Role.RETAIL_MANAGER), "Top-up approval needed - " + station.getName(),
                requestedBy + " asks for " + saved.getLitres() + " litres for " + station.getName() + ".\nReason: " + saved.getReason() + "\n",
                TOP_UPS);
        return saved;
    }

    public List<StationTopUp> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<StationTopUp> findByStation(Long stationId) {
        return repository.findByStationIdOrderByIdDesc(stationId);
    }

    public StationTopUp findById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public List<Approval> approvals(Long id) {
        findById(id);
        return approvalRepository.findBySubjectAndRequestIdOrderByApprovalDateAsc(ApprovalSubject.STATION_TOP_UP, id);
    }

    @Transactional
    // stage: the status the approver saw and was checked against; it must still be the top-up's status
    public StationTopUp decide(Long id, TopUpStatus stage, boolean approved, String approver, String comments) {
        StationTopUp topUp = findById(id);

        if (!stage.pending() || topUp.getStatus() != stage) {
            throw new IllegalStateException("This top-up has "
                    + (topUp.getStatus() == TopUpStatus.COMPLETED ? "already been approved"
                    : topUp.getStatus() == TopUpStatus.REJECTED ? "already been rejected" : "moved to the next approval")
                    + ". Refresh to see the latest.");
        }

        // first approval only passes it on to HR & Admin
        if (stage == TopUpStatus.PENDING_FIRST_APPROVAL) {
            topUp.setStatus(approved ? TopUpStatus.PENDING_APPROVAL : TopUpStatus.REJECTED);
            approvalRepository.save(Approval.record(
                    ApprovalSubject.STATION_TOP_UP, id, ApprovalLevel.REGIONAL_RETAIL, approved, approver, comments));
            if (approved) {
                notifications.notifyRoles(List.of(Role.HR_ADMIN_MANAGER), "Top-up approval needed - " + stationName(topUp),
                        topUp.getLitres() + " litres for " + stationName(topUp) + ", requested by " + topUp.getRequestedBy()
                                + ".\nFirst approved by " + approver + ". Reason: " + topUp.getReason() + "\n",
                        TOP_UPS);
            } else {
                notifyStation(topUp, false, approver, comments);
            }
            return repository.save(topUp);
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
        notifyStation(topUp, approved, approver, comments);

        return repository.save(topUp);
    }

    private static final String TOP_UPS = "/top-ups";

    private String stationName(StationTopUp topUp) {
        return stationRepository.findById(topUp.getStationId()).map(Station::getName).orElse("Station #" + topUp.getStationId());
    }

    // the station's admins hear the outcome
    private void notifyStation(StationTopUp topUp, boolean approved, String approver, String comments) {
        String station = stationName(topUp);
        notifications.notifyStationAdmins(topUp.getStationId(), "Top-up " + (approved ? "approved" : "rejected") + " - " + station,
                (approved ? topUp.getLitres() + " litres were loaded onto " + station + "'s fuel card"
                        : "The request for " + topUp.getLitres() + " litres for " + station + " was rejected")
                        + " by " + approver + ".\n" + (comments == null || comments.isBlank() ? "" : "Comment: " + comments.trim() + "\n"),
                "/stations/" + topUp.getStationId());
    }
}
