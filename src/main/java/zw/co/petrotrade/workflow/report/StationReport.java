package zw.co.petrotrade.workflow.report;

import zw.co.petrotrade.workflow.station.dto.StationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// A station's top-ups requested in a period, with what was received
public record StationReport(
        StationResponse station,
        LocalDate from,
        LocalDate to,
        List<Row> rows,
        Totals totals) {

    public record Row(
            Long topUpId,
            String station,
            LocalDateTime requestedAt,
            Double litres,
            String reason,
            String requestedBy,
            String status,
            String decidedBy,
            LocalDateTime decidedAt) {
    }

    public record Totals(int requests, double litresRequested, double litresReceived, long pending, long rejected) {
    }

    // the station page header: the station and what's on its card right now
    public record Summary(StationResponse station, String cardNumber, Double balanceLitres, Boolean cardActive, long pendingTopUps) {
    }
}
