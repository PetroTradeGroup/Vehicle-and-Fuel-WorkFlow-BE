package zw.co.petrotrade.workflow.report;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zw.co.petrotrade.workflow.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Same filters for the on-screen report and both downloads, so a file always matches what was on screen
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR_ADMIN_MANAGER', 'VEHICLE_ADMIN', 'SYSTEM_ADMIN')")
public class ReportController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final List<TableExport.Col<StationReport.Row>> TOP_UP_COLUMNS = StationReportController.columns(true);

    private final ReportService reports;
    private final StationReportService stationReports;
    private final ReportExporter exporter;
    private final CurrentUser currentUser;

    @GetMapping("/trips")
    public TripReport trips(TripReport.Filter filter) {
        return reports.trips(filter);
    }

    @GetMapping("/trips/excel")
    public ResponseEntity<byte[]> excel(TripReport.Filter filter) {
        TripReport report = reports.trips(filter);
        return download(exporter.excel(report, currentUser.get().getFullName()), XLSX, exporter.fileName(report, "xlsx"));
    }

    @GetMapping("/trips/pdf")
    public ResponseEntity<byte[]> pdf(TripReport.Filter filter) {
        TripReport report = reports.trips(filter);
        return download(exporter.pdf(report, currentUser.get().getFullName()), MediaType.APPLICATION_PDF, exporter.fileName(report, "pdf"));
    }

    // station top-ups across stations; a station admin uses their own station's page instead
    @GetMapping("/top-ups")
    public TopUpReport topUps(TopUpReport.Filter filter) {
        return stationReports.allStations(filter);
    }

    @GetMapping("/top-ups/excel")
    public ResponseEntity<byte[]> topUpsExcel(TopUpReport.Filter filter) {
        TopUpReport report = stationReports.allStations(filter);
        byte[] file = TableExport.excel("Top-ups", "Station top-ups report", topUpMeta(report), TOP_UP_COLUMNS, report.rows(),
                "Total: " + report.totals().requests() + " top-ups", Map.of("Litres", report.totals().litresReceived()));
        return download(file, XLSX, topUpFileName(report, "xlsx"));
    }

    @GetMapping("/top-ups/pdf")
    public ResponseEntity<byte[]> topUpsPdf(TopUpReport.Filter filter) {
        TopUpReport report = stationReports.allStations(filter);
        StationReport.Totals t = report.totals();
        String summary = t.requests() + " top-ups   ·   " + TableExport.format(t.litresRequested()) + " L requested   ·   "
                + TableExport.format(t.litresReceived()) + " L received   ·   " + t.pending() + " pending   ·   " + t.rejected() + " rejected";
        byte[] file = TableExport.pdf("Station top-ups report", topUpMeta(report), summary, TOP_UP_COLUMNS,
                new float[]{4, 11, 9, 9, 5, 17, 7, 9, 9}, report.rows(),
                "Total: " + t.requests() + " top-ups", Map.of("Litres", t.litresReceived()));
        return download(file, MediaType.APPLICATION_PDF, topUpFileName(report, "pdf"));
    }

    private List<String> topUpMeta(TopUpReport report) {
        return List.of(
                "Period: " + TableExport.DAY.format(report.from()) + " – " + TableExport.DAY.format(report.to())
                        + "    Filters: " + report.filterSummary() + "    (Litres total = approved top-ups only)",
                "Generated " + TableExport.STAMP.format(LocalDateTime.now()) + " by " + currentUser.get().getFullName());
    }

    private static String topUpFileName(TopUpReport report, String extension) {
        return "top-up-report_" + report.from() + "_to_" + report.to() + "." + extension;
    }

    private static ResponseEntity<byte[]> download(byte[] body, MediaType type, String fileName) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(body);
    }
}
