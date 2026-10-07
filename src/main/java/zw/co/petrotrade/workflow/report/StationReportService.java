package zw.co.petrotrade.workflow.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.fuel.CardHolderType;
import zw.co.petrotrade.workflow.fuel.FuelCard;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.station.Station;
import zw.co.petrotrade.workflow.station.StationRepository;
import zw.co.petrotrade.workflow.station.StationTopUp;
import zw.co.petrotrade.workflow.station.StationTopUpRepository;
import zw.co.petrotrade.workflow.station.TopUpStatus;
import zw.co.petrotrade.workflow.station.dto.StationResponse;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Top-up reports: one station's (its page) or across stations (the Reports page). Same rows, same totals.
@Service
@RequiredArgsConstructor
public class StationReportService {

    private static final Map<TopUpStatus, String> STATUS_LABEL = Map.of(
            TopUpStatus.PENDING_APPROVAL, "Pending approval",
            TopUpStatus.COMPLETED, "Approved",
            TopUpStatus.REJECTED, "Rejected");

    private final StationRepository stations;
    private final StationTopUpRepository topUps;
    private final FuelCardRepository cards;
    private final ApprovalRepository approvals;

    public StationReport.Summary summary(Long stationId) {
        Station station = stations.findById(stationId).orElseThrow();
        FuelCard card = cards.findByHolderTypeAndHolderId(CardHolderType.STATION, stationId).orElse(null);
        return new StationReport.Summary(
                StationResponse.from(station),
                card == null ? null : card.getCardNumber(),
                card == null ? null : card.getBalanceLitres(),
                card == null ? null : card.getActive(),
                topUps.countByStationIdAndStatus(stationId, TopUpStatus.PENDING_APPROVAL));
    }

    public StationReport report(Long stationId, LocalDate from, LocalDate to) {
        checkPeriod(from, to);
        Station station = stations.findById(stationId).orElseThrow();
        List<StationTopUp> list = topUps.findByStationIdAndRequestedAtGreaterThanEqualAndRequestedAtLessThanOrderByRequestedAtAsc(
                stationId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return new StationReport(StationResponse.from(station), from, to, rows(list), totals(list));
    }

    public TopUpReport allStations(TopUpReport.Filter filter) {
        checkPeriod(filter.from(), filter.to());
        // ponytail: station/status filters run in memory over the period's top-ups; query them if volumes grow
        List<StationTopUp> list = topUps.findByRequestedAtGreaterThanEqualAndRequestedAtLessThanOrderByRequestedAtAsc(
                        filter.from().atStartOfDay(), filter.to().plusDays(1).atStartOfDay()).stream()
                .filter(t -> filter.stationId() == null || filter.stationId().equals(t.getStationId()))
                .filter(t -> filter.status() == null || filter.status() == t.getStatus())
                .toList();

        List<String> parts = new ArrayList<>();
        if (filter.stationId() != null) {
            parts.add("Station: " + stations.findById(filter.stationId()).map(Station::getName).orElse("#" + filter.stationId()));
        }
        if (filter.status() != null) {
            parts.add("Status: " + STATUS_LABEL.get(filter.status()));
        }
        String summary = parts.isEmpty() ? "All stations" : String.join(" · ", parts);
        return new TopUpReport(filter.from(), filter.to(), summary, rows(list), totals(list));
    }

    private List<StationReport.Row> rows(List<StationTopUp> list) {
        List<Long> ids = list.stream().map(StationTopUp::getId).toList();
        Map<Long, Approval> decisionByTopUp = approvals.findBySubjectAndRequestIdIn(ApprovalSubject.STATION_TOP_UP, ids).stream()
                .collect(Collectors.toMap(Approval::getRequestId, Function.identity(), (a, b) -> b));
        Map<Long, String> stationName = stations.findAllById(list.stream().map(StationTopUp::getStationId).distinct().toList())
                .stream().collect(Collectors.toMap(Station::getId, Station::getName));

        return list.stream().map(t -> {
            Approval decision = decisionByTopUp.get(t.getId());
            return new StationReport.Row(
                    t.getId(), stationName.get(t.getStationId()), t.getRequestedAt(), t.getLitres(), t.getReason(), t.getRequestedBy(),
                    STATUS_LABEL.get(t.getStatus()),
                    decision == null ? null : decision.getApprover(),
                    decision == null ? null : decision.getApprovalDate());
        }).toList();
    }

    private static StationReport.Totals totals(List<StationTopUp> list) {
        return new StationReport.Totals(
                list.size(),
                list.stream().mapToDouble(t -> t.getLitres() == null ? 0 : t.getLitres()).sum(),
                list.stream().filter(t -> t.getStatus() == TopUpStatus.COMPLETED)
                        .mapToDouble(t -> t.getLitres() == null ? 0 : t.getLitres()).sum(),
                list.stream().filter(t -> t.getStatus() == TopUpStatus.PENDING_APPROVAL).count(),
                list.stream().filter(t -> t.getStatus() == TopUpStatus.REJECTED).count());
    }

    private static void checkPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Choose a start and end date for the report.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("The report's end date can't be before its start date.");
        }
        if (ChronoUnit.DAYS.between(from, to) > 366 * 3) {
            throw new IllegalArgumentException("Reports can cover at most three years. Choose a shorter period.");
        }
    }
}
