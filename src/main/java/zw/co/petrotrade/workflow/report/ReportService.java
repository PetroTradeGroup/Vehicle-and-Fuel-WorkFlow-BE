package zw.co.petrotrade.workflow.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalStatus;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.fuel.FuelTransaction;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.fuel.FuelTypeRepository;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.ReturnDelayService;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;
import zw.co.petrotrade.workflow.user.UserRepository;
import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    // a vehicle was assigned to the trip; rejected requests never left the yard
    private static final List<RequestStatus> TRIP_STATUSES = List.of(
            RequestStatus.VEHICLE_ALLOCATED, RequestStatus.FUEL_CALCULATED,
            RequestStatus.FUEL_APPROVED, RequestStatus.COMPLETED);

    private static final Map<RequestStatus, String> STATUS_LABEL = Map.of(
            RequestStatus.VEHICLE_ALLOCATED, "Vehicle allocated",
            RequestStatus.FUEL_CALCULATED, "Awaiting HR & Admin approval",
            RequestStatus.FUEL_APPROVED, "On trip",
            RequestStatus.COMPLETED, "Completed");

    private final TransportRequestRepository requests;
    private final VehicleAllocationRepository allocations;
    private final VehicleRepository vehicles;
    private final ApprovalRepository approvals;
    private final FuelTransactionRepository fuelTransactions;
    private final FuelTypeRepository fuelTypes;
    private final UserRepository users;

    public TripReport trips(TripReport.Filter filter) {
        if (filter.from() == null || filter.to() == null) {
            throw new IllegalArgumentException("Choose a start and end date for the report.");
        }
        if (filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("The report's end date can't be before its start date.");
        }
        if (ChronoUnit.DAYS.between(filter.from(), filter.to()) > 366 * 3) {
            throw new IllegalArgumentException("Reports can cover at most three years. Choose a shorter period.");
        }

        List<TransportRequest> trips = requests.findTripsBetween(filter.from(), filter.to(), TRIP_STATUSES);
        List<Long> ids = trips.stream().map(TransportRequest::getId).toList();

        Map<Long, VehicleAllocation> allocationByRequest = allocations.findByRequestIdIn(ids).stream()
                .collect(Collectors.toMap(VehicleAllocation::getRequestId, Function.identity(), (a, b) -> a));
        Map<Long, Vehicle> vehicleById = vehicles.findAllById(allocationByRequest.values().stream()
                        .map(VehicleAllocation::getVehicleId).toList()).stream()
                .collect(Collectors.toMap(Vehicle::getId, Function.identity()));
        Map<Long, List<Approval>> approvalsByRequest = approvals
                .findBySubjectAndRequestIdIn(ApprovalSubject.TRANSPORT_REQUEST, ids).stream()
                .collect(Collectors.groupingBy(Approval::getRequestId));
        Map<Long, Double> litresByRequest = fuelTransactions.findByRequestIdIn(ids).stream()
                .collect(Collectors.groupingBy(FuelTransaction::getRequestId,
                        Collectors.summingDouble(t -> t.getAllocatedLitres() == null ? 0 : t.getAllocatedLitres())));

        // ponytail: filters run in memory over the period's trips; move them into the query if periods get huge
        List<TripReport.Row> rows = new ArrayList<>();
        LocalDate today = LocalDate.now(ReturnDelayService.ZONE);
        for (TransportRequest trip : trips) {
            VehicleAllocation allocation = allocationByRequest.get(trip.getId());
            Vehicle vehicle = allocation == null ? null : vehicleById.get(allocation.getVehicleId());
            List<Approval> decisions = approvalsByRequest.getOrDefault(trip.getId(), List.of());

            if (filter.vehicleId() != null && (vehicle == null || !filter.vehicleId().equals(vehicle.getId()))) continue;
            if (filter.driverId() != null && !filter.driverId().equals(trip.getDriverId())) continue;
            if (filter.fuelTypeId() != null && (trip.getFuelType() == null || !filter.fuelTypeId().equals(trip.getFuelType().getId()))) continue;
            if (filter.approver() != null && !filter.approver().isBlank() && decisions.stream()
                    .noneMatch(a -> filter.approver().trim().equalsIgnoreCase(a.getApprover()))) continue;
            Long daysLate = daysLate(trip, allocation, today);
            if (Boolean.TRUE.equals(filter.lateOnly()) && daysLate == null) continue;

            rows.add(new TripReport.Row(
                    trip.getId(),
                    trip.getRequiredFrom(),
                    trip.getRequiredTo(),
                    trip.getDriverName(),
                    trip.getDepartment(),
                    vehicle == null ? null : vehicle.getRegistrationNumber(),
                    vehicle == null ? null : vehicle.getMake() + " " + vehicle.getModel(),
                    trip.getStartingPoint() + " → " + trip.getDestination(),
                    trip.getDistanceKm(),
                    allocation == null ? null : allocation.getDistanceTravelledKm(),
                    trip.getFuelType() == null ? null : trip.getFuelType().getName(),
                    litresByRequest.get(trip.getId()),
                    allocation == null ? null : allocation.getFuelUsedLitres(),
                    approver(decisions, ApprovalLevel.HOD),
                    approver(decisions, ApprovalLevel.HR_ADMIN),
                    approver(decisions, ApprovalLevel.FUEL),
                    allocation == null ? null : allocation.getAllocatedBy(),
                    allocation == null ? null : allocation.getReturnDate(),
                    STATUS_LABEL.get(trip.getStatus()),
                    daysLate,
                    trip.getDelayReason()));
        }

        TripReport.Totals totals = new TripReport.Totals(
                rows.size(),
                sum(rows, TripReport.Row::distanceKm),
                sum(rows, TripReport.Row::distanceTravelledKm),
                sum(rows, TripReport.Row::litresIssued),
                sum(rows, TripReport.Row::fuelUsedLitres),
                rows.stream().map(TripReport.Row::vehicle).filter(Objects::nonNull).distinct().count(),
                rows.stream().map(TripReport.Row::driver).filter(Objects::nonNull).distinct().count(),
                rows.stream().filter(r -> r.daysLate() != null).count());

        return new TripReport(filter.from(), filter.to(), describe(filter), rows, totals,
                leaders(rows, TripReport.Row::driver, TripReport.Row::department,
                        Comparator.comparingDouble(TripReport.Leader::fuelUsedLitres)
                                .thenComparingDouble(TripReport.Leader::litresIssued)),
                UsageReport.By.VEHICLE.rank(rows).stream().limit(LEADERS).toList());
    }

    // same period and filters as the trip report, ranked in full
    public UsageReport usage(TripReport.Filter filter, UsageReport.By by) {
        TripReport report = trips(filter);
        return new UsageReport(report.from(), report.to(), report.filterSummary(), by, by.rank(report.rows()));
    }

    static final int LEADERS = 5;

    // the top LEADERS of rank()
    static List<TripReport.Leader> leaders(List<TripReport.Row> rows, Function<TripReport.Row, String> key,
                                           Function<TripReport.Row, String> detail, Comparator<TripReport.Leader> order) {
        return rank(rows, key, detail, order).stream().limit(LEADERS).toList();
    }

    // groups the rows by key, highest first by the given order
    static List<TripReport.Leader> rank(List<TripReport.Row> rows, Function<TripReport.Row, String> key,
                                        Function<TripReport.Row, String> detail, Comparator<TripReport.Leader> order) {
        return rows.stream()
                .filter(r -> key.apply(r) != null)
                .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new TripReport.Leader(
                        e.getKey(),
                        detail.apply(e.getValue().get(0)),
                        e.getValue().size(),
                        e.getValue().stream().mapToDouble(r -> r.distanceTravelledKm() != null ? r.distanceTravelledKm()
                                : r.distanceKm() == null ? 0 : r.distanceKm()).sum(),
                        sum(e.getValue(), TripReport.Row::fuelUsedLitres),
                        sum(e.getValue(), TripReport.Row::litresIssued)))
                .sorted(order.reversed())
                .toList();
    }

    // days past the (possibly extended) return date: when it came back, or today while it's still out; null when on time
    static Long daysLate(TransportRequest trip, VehicleAllocation allocation, LocalDate today) {
        if (trip.getRequiredTo() == null) return null;
        LocalDate back = allocation != null && allocation.getReturnDate() != null ? allocation.getReturnDate().toLocalDate()
                : trip.getStatus() == RequestStatus.FUEL_APPROVED ? today : null;
        long days = back == null ? 0 : ChronoUnit.DAYS.between(trip.getRequiredTo(), back);
        return days > 0 ? days : null;
    }

    private static double sum(List<TripReport.Row> rows, Function<TripReport.Row, Double> field) {
        return rows.stream().map(field).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
    }

    private static String approver(List<Approval> decisions, ApprovalLevel level) {
        return decisions.stream()
                .filter(a -> a.getLevel() == level && a.getStatus() == ApprovalStatus.APPROVED)
                .map(Approval::getApprover)
                .findFirst().orElse(null);
    }

    // "Vehicle: AZW 6789 · Driver: Jane Moyo" — printed on the exports so a file says what it covers
    private String describe(TripReport.Filter filter) {
        List<String> parts = new ArrayList<>();
        if (filter.vehicleId() != null) {
            parts.add("Vehicle: " + vehicles.findById(filter.vehicleId()).map(Vehicle::getRegistrationNumber).orElse("#" + filter.vehicleId()));
        }
        if (filter.driverId() != null) {
            parts.add("Driver: " + users.findById(filter.driverId()).map(u -> u.getFullName()).orElse("#" + filter.driverId()));
        }
        if (filter.fuelTypeId() != null) {
            parts.add("Fuel type: " + fuelTypes.findById(filter.fuelTypeId()).map(t -> t.getName()).orElse("#" + filter.fuelTypeId()));
        }
        if (filter.approver() != null && !filter.approver().isBlank()) {
            parts.add("Approver: " + filter.approver().trim());
        }
        if (Boolean.TRUE.equals(filter.lateOnly())) {
            parts.add("Late returns only");
        }
        return parts.isEmpty() ? "All trips" : String.join(" · ", parts);
    }
}
