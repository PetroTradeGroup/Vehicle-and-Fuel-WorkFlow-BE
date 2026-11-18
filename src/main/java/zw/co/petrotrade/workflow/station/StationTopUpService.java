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
import zw.co.petrotrade.workflow.fuel.FuelTypeRepository;
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
    private final FuelTypeRepository fuelTypeRepository;

    public StationTopUp create(StationTopUpRequest request, String requestedBy) {
        Station station = stationRepository.findById(request.stationId()).orElseThrow();
        // fail now rather than at approval if the station has nothing to credit
        fuelCardService.findActiveCard(CardHolderType.STATION, request.stationId());
        boolean generator = request.purpose() == TopUpPurpose.GENERATOR;
        if (request.purpose() == TopUpPurpose.GRASS_CUTTER && request.litres() > TopUpPurpose.GRASS_CUTTER_MAX_LITRES) {
            throw new IllegalArgumentException(String.format(
                    "A grass cutter top-up can be at most %.0f litres.", TopUpPurpose.GRASS_CUTTER_MAX_LITRES));
        }
        if (generator && request.generatorHours() == null) {
            throw new IllegalArgumentException("Enter the generator's running hours.");
        }
        if (generator) {
            if (station.getGeneratorHours() != null && request.generatorHours() < station.getGeneratorHours()) {
                throw new IllegalArgumentException(String.format(
                        "The generator can't read less than %.1f hours, the last reading for %s.",
                        station.getGeneratorHours(), station.getName()));
            }
            station.setGeneratorHours(request.generatorHours());
            stationRepository.save(station);
        }

        StationTopUp topUp = new StationTopUp();
        topUp.setStationId(request.stationId());
        topUp.setLitres(request.litres());
        topUp.setFuelType(fuelTypeRepository.findById(request.fuelTypeId()).orElseThrow());
        topUp.setPurpose(request.purpose());
        topUp.setGeneratorHours(generator ? request.generatorHours() : null);
        topUp.setReason(request.reason());
        topUp.setRequestedBy(requestedBy);
        topUp.setRequestedAt(LocalDateTime.now());
        topUp.setStatus(TopUpStatus.PENDING_FIRST_APPROVAL);
        StationTopUp saved = repository.save(topUp);
        notifications.notifyRoles(List.of(Role.REGIONAL_REP, Role.RETAIL_MANAGER), "Top-up approval needed - " + station.getName(),
                requestedBy + " asks for " + saved.getLitres() + " litres of " + saved.getFuelType().getName() + " for " + station.getName() + ".\n"
                        + "For: " + saved.getPurpose().label() + "\n"
                        + (generator ? "Generator hours: " + saved.getGeneratorHours() + "\n" : "")
                        + "Reason: " + saved.getReason() + "\n",
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
                    + (topUp.getStatus() == TopUpStatus.APPROVED || topUp.getStatus() == TopUpStatus.COMPLETED ? "already been approved"
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
            // like trip fuel: the card is picked now but only credited once IT has loaded the fuel onto it
            FuelCard card = fuelCardService.findActiveCard(CardHolderType.STATION, topUp.getStationId());

            FuelTransaction transaction = new FuelTransaction();
            transaction.setStationTopUpId(id);
            transaction.setFuelCardId(card.getId());
            transaction.setAllocatedLitres(topUp.getLitres());
            transaction.setConsumedLitres(0.0);
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setLoaded(false);
            fuelTransactionRepository.save(transaction);

            // completed once IT marks it loaded (FuelApprovalService.markLoaded)
            topUp.setStatus(TopUpStatus.APPROVED);
            notifications.notifyRoles(List.of(Role.SYSTEM_ADMIN), "Fuel to load - " + stationName(topUp),
                    "HR & Admin approved " + topUp.getLitres() + " litres"
                            + (topUp.getFuelType() == null ? "" : " of " + topUp.getFuelType().getName()) + " for " + stationName(topUp)
                            + ". Load it onto fuel card " + card.getCardNumber() + ", then mark it loaded.\n",
                    "/fuel-loading");
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
                (approved ? topUp.getLitres() + " litres for " + station + " were approved; IT will load them onto the fuel card and you'll be told when it's done. Approved"
                        : "The request for " + topUp.getLitres() + " litres for " + station + " was rejected")
                        + " by " + approver + ".\n" + (comments == null || comments.isBlank() ? "" : "Comment: " + comments.trim() + "\n"),
                "/stations/" + topUp.getStationId());
    }
}
