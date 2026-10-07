package zw.co.petrotrade.workflow.report;

import org.springframework.format.annotation.DateTimeFormat;
import zw.co.petrotrade.workflow.station.TopUpStatus;

import java.time.LocalDate;
import java.util.List;

// Station top-ups across stations for a period (the Reports page); rows and totals as on a station's page
public record TopUpReport(
        LocalDate from,
        LocalDate to,
        String filterSummary,
        List<StationReport.Row> rows,
        StationReport.Totals totals) {

    public record Filter(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Long stationId,
            TopUpStatus status) {
    }
}
