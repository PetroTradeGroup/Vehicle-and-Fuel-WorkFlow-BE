package zw.co.petrotrade.workflow.report;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// One trip report: the trips whose dates overlap the period, after the optional filters
public record TripReport(
        LocalDate from,
        LocalDate to,
        String filterSummary,
        List<Row> rows,
        Totals totals,
        // drivers who used the most fuel, and the vehicles that went out most, over the same rows
        List<Leader> topFuelUsers,
        List<Leader> mostUsedVehicles) {

    // km is what was recorded on return, else the planned distance
    public record Leader(String name, String detail, long trips, double distanceKm, double fuelUsedLitres,
                         double litresIssued) {
    }

    // query parameters shared by the JSON, Excel and PDF endpoints
    public record Filter(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Long vehicleId,
            Long driverId,
            Long fuelTypeId,
            String approver,
            // only trips that came back after their return date, or are still out past it
            Boolean lateOnly) {
    }

    public record Row(
            Long requestId,
            LocalDate from,
            LocalDate to,
            String driver,
            String department,
            String vehicle,
            String vehicleModel,
            String route,
            Double distanceKm,
            // recorded when the vehicle came back (optional there)
            Double distanceTravelledKm,
            String fuelType,
            // fuel loaded onto a card for the trip
            Double litresIssued,
            // fuel actually drawn, recorded on return
            Double fuelUsedLitres,
            String hodApprover,
            String hrApprover,
            String fuelApprover,
            String allocatedBy,
            LocalDateTime returnedAt,
            String status,
            // days past the return date it came back, or has been out so far; null when on time
            Long daysLate,
            // what the driver said about the delay
            String delayReason) {
    }

    public record Totals(int trips, double distanceKm, double distanceTravelledKm, double litresIssued,
                         double fuelUsedLitres, long vehicles, long drivers, long lateReturns) {
    }
}
