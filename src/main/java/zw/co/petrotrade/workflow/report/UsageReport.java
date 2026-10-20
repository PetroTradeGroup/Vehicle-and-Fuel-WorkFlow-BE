package zw.co.petrotrade.workflow.report;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

// The trip report's rows ranked by vehicle, department or person, every one of them, highest first
public record UsageReport(LocalDate from, LocalDate to, String filterSummary, By by, List<TripReport.Leader> rows) {

    public enum By {
        VEHICLE("Most used vehicles", "Vehicle", "vehicles", "Model", "trips, then distance",
                TripReport.Row::vehicle, TripReport.Row::vehicleModel,
                Comparator.comparingLong(TripReport.Leader::trips).thenComparingDouble(TripReport.Leader::distanceKm)),
        DEPARTMENT("Fuel use by department", "Department", "departments", null, "fuel used, then fuel issued",
                TripReport.Row::department, r -> null,
                Comparator.comparingDouble(TripReport.Leader::fuelUsedLitres).thenComparingDouble(TripReport.Leader::litresIssued)),
        DRIVER("Distance travelled by person", "Person", "people", "Department", "distance, then trips",
                TripReport.Row::driver, TripReport.Row::department,
                Comparator.comparingDouble(TripReport.Leader::distanceKm).thenComparingLong(TripReport.Leader::trips));

        public final String title;
        public final String subject;
        public final String plural;
        // heading of the second column; null when there is none
        public final String detailTitle;
        public final String rankedBy;
        final Function<TripReport.Row, String> key;
        final Function<TripReport.Row, String> detail;
        final Comparator<TripReport.Leader> order;

        By(String title, String subject, String plural, String detailTitle, String rankedBy, Function<TripReport.Row, String> key,
           Function<TripReport.Row, String> detail, Comparator<TripReport.Leader> order) {
            this.title = title;
            this.subject = subject;
            this.plural = plural;
            this.detailTitle = detailTitle;
            this.rankedBy = rankedBy;
            this.key = key;
            this.detail = detail;
            this.order = order;
        }

        public List<TripReport.Leader> rank(List<TripReport.Row> rows) {
            return ReportService.rank(rows, key, detail, order);
        }
    }
}
