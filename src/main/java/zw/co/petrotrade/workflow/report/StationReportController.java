package zw.co.petrotrade.workflow.report;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.co.petrotrade.workflow.security.CurrentUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static zw.co.petrotrade.workflow.report.TableExport.DAY;
import static zw.co.petrotrade.workflow.report.TableExport.STAMP;
import static zw.co.petrotrade.workflow.report.TableExport.format;

// A station's page: its card and pending top-ups, and the top-up report for a period.
// Station admins only reach their own station; HR, vehicle and system admins any station.
@RestController
@RequestMapping("/api/stations/{id}")
@RequiredArgsConstructor
public class StationReportController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final List<TableExport.Col<StationReport.Row>> COLUMNS = columns(false);

    // also used by the all-stations top-up report, which adds a Station column
    static List<TableExport.Col<StationReport.Row>> columns(boolean withStation) {
        List<TableExport.Col<StationReport.Row>> cols = new java.util.ArrayList<>(List.of(
                new TableExport.Col<>("Top-up #", 9, StationReport.Row::topUpId),
                new TableExport.Col<>("Requested", 17, StationReport.Row::requestedAt),
                new TableExport.Col<>("Requested by", 20, StationReport.Row::requestedBy),
                new TableExport.Col<>("Litres", 10, StationReport.Row::litres),
                new TableExport.Col<>("Reason", 34, StationReport.Row::reason),
                new TableExport.Col<>("Status", 16, StationReport.Row::status),
                new TableExport.Col<>("Decided by", 20, StationReport.Row::decidedBy),
                new TableExport.Col<>("Decided", 17, StationReport.Row::decidedAt)));
        if (withStation) {
            cols.add(1, new TableExport.Col<>("Station", 22, StationReport.Row::station));
        }
        return cols;
    }

    private final StationReportService reports;
    private final CurrentUser currentUser;

    @GetMapping("/summary")
    public StationReport.Summary summary(@PathVariable Long id) {
        currentUser.requireStation(id);
        return reports.summary(id);
    }

    @GetMapping("/report")
    public StationReport report(@PathVariable Long id,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        currentUser.requireStation(id);
        return reports.report(id, from, to);
    }

    @GetMapping("/report/excel")
    public ResponseEntity<byte[]> excel(@PathVariable Long id,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        currentUser.requireStation(id);
        StationReport report = reports.report(id, from, to);
        byte[] file = TableExport.excel("Top-ups", title(report), meta(report), COLUMNS, report.rows(), totalLabel(report), totals(report));
        return download(file, XLSX, fileName(report, "xlsx"));
    }

    @GetMapping("/report/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        currentUser.requireStation(id);
        StationReport report = reports.report(id, from, to);
        StationReport.Totals t = report.totals();
        String summary = t.requests() + " top-ups   ·   " + format(t.litresRequested()) + " L requested   ·   "
                + format(t.litresReceived()) + " L received   ·   " + t.pending() + " pending   ·   " + t.rejected() + " rejected";
        byte[] file = TableExport.pdf(title(report), meta(report), summary, COLUMNS, new float[]{4, 9, 10, 5, 20, 7, 10, 9},
                report.rows(), totalLabel(report), totals(report));
        return download(file, MediaType.APPLICATION_PDF, fileName(report, "pdf"));
    }

    private static String title(StationReport report) {
        return report.station().name() + " – station top-ups";
    }

    private List<String> meta(StationReport report) {
        return List.of(
                "Period: " + DAY.format(report.from()) + " – " + DAY.format(report.to())
                        + (report.station().location() == null ? "" : "    Location: " + report.station().location()),
                "Generated " + STAMP.format(LocalDateTime.now()) + " by " + currentUser.get().getFullName());
    }

    private static String totalLabel(StationReport report) {
        return "Total: " + report.totals().requests() + " top-ups";
    }

    // the Litres total counts what was actually received (approved), not what was asked for
    private static Map<String, Double> totals(StationReport report) {
        return Map.of("Litres", report.totals().litresReceived());
    }

    private static String fileName(StationReport report, String extension) {
        String slug = report.station().name().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return "station-report_" + slug + "_" + report.from() + "_to_" + report.to() + "." + extension;
    }

    private static ResponseEntity<byte[]> download(byte[] body, MediaType type, String fileName) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(body);
    }
}
