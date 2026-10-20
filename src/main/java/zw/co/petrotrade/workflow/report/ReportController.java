package zw.co.petrotrade.workflow.report;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.vehicle.ServiceStatus;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final List<TableExport.Col<VehicleResponse>> SERVICE_COLUMNS = List.of(
            new TableExport.Col<>("Registration", 13, VehicleResponse::registrationNumber),
            new TableExport.Col<>("Make / model", 20, v -> v.make() + " " + v.model()),
            new TableExport.Col<>("Odometer (km)", 13, VehicleResponse::odometerKm),
            new TableExport.Col<>("Last service (km)", 13, VehicleResponse::lastServiceOdometerKm),
            new TableExport.Col<>("Last service date", 14, VehicleResponse::lastServiceDate),
            new TableExport.Col<>("Next service (km)", 13, v -> v.lastServiceOdometerKm() == null ? null
                    : v.lastServiceOdometerKm() + ServiceStatus.INTERVAL_KM),
            new TableExport.Col<>("Km to service", 12, VehicleResponse::kmToService),
            new TableExport.Col<>("Service", 18, v -> serviceLabel(v.serviceStatus())));

    private final ReportService reports;
    private final StationReportService stationReports;
    private final VehicleRepository vehicles;
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

    // vehicles by trips, departments by fuel, people by distance, over the trip report's filters
    @GetMapping("/usage")
    public UsageReport usage(TripReport.Filter filter, @RequestParam UsageReport.By by) {
        return reports.usage(filter, by);
    }

    @GetMapping("/usage/excel")
    public ResponseEntity<byte[]> usageExcel(TripReport.Filter filter, @RequestParam UsageReport.By by) {
        UsageReport report = reports.usage(filter, by);
        byte[] file = TableExport.excel(by.subject, by.title, usageMeta(report), usageColumns(report), report.rows(),
                usageTotalLabel(report), usageTotals(report));
        return download(file, XLSX, usageFileName(report, "xlsx"));
    }

    @GetMapping("/usage/pdf")
    public ResponseEntity<byte[]> usagePdf(TripReport.Filter filter, @RequestParam UsageReport.By by) {
        UsageReport report = reports.usage(filter, by);
        String summary = report.rows().isEmpty() ? "No trips in this period"
                : "Top: " + report.rows().get(0).name() + "   ·   " + usageTotalLabel(report);
        float[] widths = by.detailTitle == null ? new float[]{4, 26, 8, 12, 12, 12} : new float[]{4, 18, 18, 8, 12, 12, 12};
        byte[] file = TableExport.pdf(by.title, usageMeta(report), summary, usageColumns(report), widths, report.rows(),
                usageTotalLabel(report), usageTotals(report));
        return download(file, MediaType.APPLICATION_PDF, usageFileName(report, "pdf"));
    }

    private static List<TableExport.Col<TripReport.Leader>> usageColumns(UsageReport report) {
        List<TripReport.Leader> rows = report.rows();
        List<TableExport.Col<TripReport.Leader>> cols = new ArrayList<>();
        // ponytail: indexOf per row is O(n²); fine for a fleet's worth of vehicles, departments or people
        cols.add(new TableExport.Col<>("#", 5, l -> (long) rows.indexOf(l) + 1));
        cols.add(new TableExport.Col<>(report.by().subject, 22, TripReport.Leader::name));
        if (report.by().detailTitle != null) {
            cols.add(new TableExport.Col<>(report.by().detailTitle, 22, TripReport.Leader::detail));
        }
        cols.add(new TableExport.Col<>("Trips", 8, TripReport.Leader::trips));
        cols.add(new TableExport.Col<>("Distance (km)", 13, TripReport.Leader::distanceKm));
        cols.add(new TableExport.Col<>("Fuel used (L)", 13, TripReport.Leader::fuelUsedLitres));
        cols.add(new TableExport.Col<>("Fuel issued (L)", 13, TripReport.Leader::litresIssued));
        return cols;
    }

    private static Map<String, Double> usageTotals(UsageReport report) {
        return Map.of(
                "Distance (km)", report.rows().stream().mapToDouble(TripReport.Leader::distanceKm).sum(),
                "Fuel used (L)", report.rows().stream().mapToDouble(TripReport.Leader::fuelUsedLitres).sum(),
                "Fuel issued (L)", report.rows().stream().mapToDouble(TripReport.Leader::litresIssued).sum());
    }

    private static String usageTotalLabel(UsageReport report) {
        long trips = report.rows().stream().mapToLong(TripReport.Leader::trips).sum();
        return report.rows().size() + " " + (report.rows().size() == 1 ? report.by().subject.toLowerCase() : report.by().plural)
                + ", " + trips + " trip" + (trips == 1 ? "" : "s");
    }

    private List<String> usageMeta(UsageReport report) {
        return List.of(
                "Period: " + TableExport.DAY.format(report.from()) + " – " + TableExport.DAY.format(report.to())
                        + "    Filters: " + report.filterSummary() + "    Ranked by " + report.by().rankedBy,
                "Distance is the odometer distance where recorded, otherwise the planned distance. Fuel used is recorded on return.",
                "Generated " + TableExport.STAMP.format(LocalDateTime.now()) + " by " + currentUser.get().getFullName());
    }

    private static String usageFileName(UsageReport report, String extension) {
        return "usage-" + report.by().name().toLowerCase() + "_" + report.from() + "_to_" + report.to() + "." + extension;
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

    // every vehicle, the closest to (or furthest past) its next service first; no service on record goes last
    @GetMapping("/service")
    public List<VehicleResponse> service() {
        return vehicles.findAll().stream().map(VehicleResponse::from)
                .sorted(Comparator.comparing(VehicleResponse::kmToService, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @GetMapping("/service/excel")
    public ResponseEntity<byte[]> serviceExcel() {
        List<VehicleResponse> rows = service();
        byte[] file = TableExport.excel("Service", "Vehicle service report", serviceMeta(), SERVICE_COLUMNS, rows,
                rows.size() + " vehicles", Map.of());
        return download(file, XLSX, "service-report_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/service/pdf")
    public ResponseEntity<byte[]> servicePdf() {
        List<VehicleResponse> rows = service();
        long due = rows.stream().filter(v -> v.serviceStatus() == ServiceStatus.DUE).count();
        long soon = rows.stream().filter(v -> v.serviceStatus() == ServiceStatus.DUE_SOON).count();
        byte[] file = TableExport.pdf("Vehicle service report", serviceMeta(),
                rows.size() + " vehicles   ·   " + due + " due for service   ·   " + soon + " due soon",
                SERVICE_COLUMNS, new float[]{9, 14, 9, 9, 9, 9, 9, 10}, rows, rows.size() + " vehicles", Map.of());
        return download(file, MediaType.APPLICATION_PDF, "service-report_" + LocalDate.now() + ".pdf");
    }

    private List<String> serviceMeta() {
        return List.of(
                "Service every " + TableExport.format(ServiceStatus.INTERVAL_KM) + " km from the last service. Due soon = within "
                        + TableExport.format(ServiceStatus.DUE_SOON_WITHIN_KM) + " km.",
                "Generated " + TableExport.STAMP.format(LocalDateTime.now()) + " by " + currentUser.get().getFullName());
    }

    private static String serviceLabel(ServiceStatus status) {
        return switch (status) {
            case OK -> "OK";
            case DUE_SOON -> "Due soon";
            case DUE -> "Due for service";
            case NOT_RECORDED -> "No service recorded";
        };
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
