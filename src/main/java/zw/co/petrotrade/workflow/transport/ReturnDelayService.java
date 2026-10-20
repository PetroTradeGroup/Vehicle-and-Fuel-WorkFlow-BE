package zw.co.petrotrade.workflow.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Vehicles kept past their return date: the driver asks for more time or explains, a vehicle admin decides,
// and every morning the vehicle admins get the list of vehicles still out late
@Service
@RequiredArgsConstructor
public class ReturnDelayService {

    public static final ZoneId ZONE = ZoneId.of("Africa/Harare");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final TransportRequestRepository requests;
    private final VehicleAllocationRepository allocations;
    private final VehicleRepository vehicles;
    private final ApprovalRepository approvals;
    private final NotificationService notifications;

    // out on the trip with the return date behind us
    public static boolean overdue(TransportRequest request, LocalDate today) {
        return request.getStatus() == RequestStatus.FUEL_APPROVED && request.getRequiredTo() != null
                && request.getRequiredTo().isBefore(today);
    }

    public static boolean overdue(TransportRequest request) {
        return overdue(request, LocalDate.now(ZONE));
    }

    // with a date it asks to keep the vehicle until then; without one it only explains the delay
    public TransportRequest requestExtension(Long requestId, LocalDate returnDate, String reason) {
        TransportRequest request = requests.findById(requestId).orElseThrow();
        if (request.getStatus() != RequestStatus.FUEL_APPROVED) {
            throw new IllegalStateException("Only a trip that's out can be extended or explained. " + request.getStatus().alreadyMovedOn());
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Say why the vehicle needs more time or why it's late.");
        }
        if (returnDate != null && !returnDate.isAfter(request.getRequiredTo())) {
            throw new IllegalArgumentException("The new return date must be after " + DAY.format(request.getRequiredTo()) + ".");
        }
        if (returnDate != null) {
            request.setExtensionRequestedTo(returnDate);
        }
        request.setDelayReason(reason.trim());
        requests.save(request);

        notifications.notifyVehicleAdmins(
                (returnDate != null ? "Extension requested" : "Late return explained") + " - Request #" + request.getId(),
                request.getDriverName() + (returnDate != null
                        ? " asks to keep the vehicle until " + DAY.format(returnDate) + " (due back " + DAY.format(request.getRequiredTo()) + ").\n"
                        : " explained why the vehicle isn't back yet (due back " + DAY.format(request.getRequiredTo()) + ").\n")
                        + "Reason: " + request.getDelayReason() + "\n\n"
                        + (returnDate != null ? "Approve or reject it on the request." : ""),
                NotificationService.requestLink(request.getId()));
        return request;
    }

    public TransportRequest decideExtension(Long requestId, boolean approved, String approver, String comments) {
        TransportRequest request = requests.findById(requestId).orElseThrow();
        if (request.getStatus() != RequestStatus.FUEL_APPROVED || request.getExtensionRequestedTo() == null) {
            throw new IllegalStateException("There's no extension request waiting on this trip. Refresh to see the latest.");
        }
        LocalDate asked = request.getExtensionRequestedTo();
        String note = "Asked to keep the vehicle from " + DAY.format(request.getRequiredTo()) + " to " + DAY.format(asked)
                + (comments == null || comments.isBlank() ? "" : ". " + comments.trim());
        if (approved) {
            request.setRequiredTo(asked);
        }
        request.setExtensionRequestedTo(null);
        requests.save(request);
        approvals.save(Approval.record(ApprovalSubject.TRANSPORT_REQUEST, requestId, ApprovalLevel.EXTENSION, approved, approver, note));

        notifications.notifyUser(request.getDriverId(), "Extension " + (approved ? "approved" : "rejected") + " - Request #" + requestId,
                approved
                        ? "You can keep the vehicle until " + DAY.format(asked) + ".\n"
                        : "Your request to keep the vehicle until " + DAY.format(asked) + " was rejected. It was due back "
                                + DAY.format(request.getRequiredTo()) + ".\n"
                                + (comments == null || comments.isBlank() ? "" : "Comment from " + approver + ": " + comments.trim() + "\n"),
                NotificationService.requestLink(requestId));
        return request;
    }

    // 8am daily: one list to the vehicle admins, and a reminder to each driver
    @Scheduled(cron = "${workflow.notifications.overdue-cron:0 0 8 * * *}", zone = "Africa/Harare")
    public void alertOverdueReturns() {
        LocalDate today = LocalDate.now(ZONE);
        List<TransportRequest> late = requests.findByStatusAndRequiredToBeforeOrderByRequiredTo(RequestStatus.FUEL_APPROVED, today);
        if (late.isEmpty()) return;

        Map<Long, VehicleAllocation> allocationByRequest = allocations.findByRequestIdIn(late.stream().map(TransportRequest::getId).toList())
                .stream().collect(Collectors.toMap(VehicleAllocation::getRequestId, Function.identity(), (a, b) -> a));
        Map<Long, String> regById = vehicles.findAllById(allocationByRequest.values().stream().map(VehicleAllocation::getVehicleId).toList())
                .stream().collect(Collectors.toMap(Vehicle::getId, Vehicle::getRegistrationNumber));

        String list = late.stream().map(r -> {
            VehicleAllocation allocation = allocationByRequest.get(r.getId());
            String reg = allocation == null ? "Vehicle" : regById.getOrDefault(allocation.getVehicleId(), "Vehicle");
            long days = ChronoUnit.DAYS.between(r.getRequiredTo(), today);
            return "- " + reg + " with " + r.getDriverName() + " (request #" + r.getId() + "), due back "
                    + DAY.format(r.getRequiredTo()) + ", " + days + " day" + (days == 1 ? "" : "s") + " late"
                    + (r.getExtensionRequestedTo() != null ? ". Extension to " + DAY.format(r.getExtensionRequestedTo()) + " awaiting your decision" : "")
                    + (r.getDelayReason() != null ? ". Driver says: " + r.getDelayReason() : ". No explanation yet");
        }).collect(Collectors.joining("\n"));
        notifications.notifyVehicleAdmins(late.size() + " vehicle" + (late.size() == 1 ? " is" : "s are") + " past their return date",
                "These vehicles should have been back by now:\n\n" + list + "\n", "/requests?status=FUEL_APPROVED");

        for (TransportRequest r : late) {
            if (r.getDriverId() == null || r.getExtensionRequestedTo() != null) continue;
            notifications.notifyUser(r.getDriverId(), "Vehicle overdue - Request #" + r.getId(),
                    "The vehicle for your trip to " + r.getDestination() + " was due back on " + DAY.format(r.getRequiredTo()) + ".\n"
                            + "Return it, or open the request to ask for more time or explain the delay.\n",
                    NotificationService.requestLink(r.getId()));
        }
    }
}
