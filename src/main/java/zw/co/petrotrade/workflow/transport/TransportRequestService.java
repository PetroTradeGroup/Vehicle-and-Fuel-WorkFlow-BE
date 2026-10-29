package zw.co.petrotrade.workflow.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.fuel.FuelCalculationService;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.user.UserRepository;

@Service
@RequiredArgsConstructor
public class TransportRequestService {

    private final TransportRequestRepository repository;
    private final UserRepository userRepository;
    private final FuelCalculationService fuelCalculationService;
    private final NotificationService notificationService;
    private final ApprovalRepository approvalRepository;

    public TransportRequest create(TransportRequest request) {

        User driver = userRepository.findById(request.getDriverId()).orElseThrow();
        request.setDriverName(driver.getFullName());
        request.setDepartment(driver.getDepartment() == null ? null : driver.getDepartment().getName());
        request.setDepartmentId(driver.getDepartment() == null ? null : driver.getDepartment().getId());
        request.setJobTitle(driver.getJobTitle());
        request.setLicenceNumber(driver.getLicenceNumber());

        request.setFuelRequiredLitres(fuelCalculationService.calculateFuel(request.getDistanceKm()));
        String skipReason = hodSkipReason(driver);
        request.setStatus(skipReason == null ? RequestStatus.PENDING_HOD : RequestStatus.APPROVED_HOD);
        TransportRequest saved = repository.save(request);
        String trip = saved.getDriverName() + ": " + saved.getStartingPoint() + " to " + saved.getDestination()
                + ", " + saved.getRequiredFrom() + (saved.getRequiredTo() == null || saved.getRequiredTo().equals(saved.getRequiredFrom()) ? "" : " to " + saved.getRequiredTo()) + ".\n";
        if (skipReason != null) {
            approvalRepository.save(Approval.record(ApprovalSubject.TRANSPORT_REQUEST, saved.getId(), ApprovalLevel.HOD,
                    true, "Automatic", skipReason));
            notificationService.notifyVehicleAdmins("Vehicle needed - Request #" + saved.getId(),
                    trip + skipReason + "\nAllocate a vehicle.\n", NotificationService.requestLink(saved.getId()));
        } else {
            notificationService.notifyDepartmentHeads(saved.getDepartmentId(), "Approval needed - Request #" + saved.getId(),
                    "A transport request from " + saved.getDepartment() + " is waiting for your approval.\n" + trip
                            + "Purpose: " + saved.getPurpose() + "\n",
                    NotificationService.requestLink(saved.getId()));
        }
        notificationService.notifyUser(saved.getDriverId(), "Request #" + saved.getId() + " submitted",
                "Your transport request was received.\n" + trip
                        + (skipReason != null ? skipReason + "\n" : "It is waiting for your head of department's approval.\n"),
                NotificationService.requestLink(saved.getId()));
        return saved;
    }

    // The department head step is skipped when nobody else could approve it; HR & Admin still approve every request at the end
    private String hodSkipReason(User driver) {
        if (driver.getDepartment() == null) {
            return driver.getFullName() + " has no department, so this went straight to vehicle allocation.";
        }
        String department = driver.getDepartment().getName();
        if (driver.getRole() != null && driver.getRole().headsDepartment()) {
            return "Requested by the head of " + department + ", so this went straight to vehicle allocation.";
        }
        if (userRepository.findByRoleInAndDepartment_Id(Role.DEPARTMENT_HEADS, driver.getDepartment().getId()).isEmpty()) {
            return department + " has no head of department, so this went straight to vehicle allocation.";
        }
        return null;
    }

    // litres is the vehicle admin's call after checking the tank and card (0 = it has enough), with a
    // reason when it's more than calculated. null loads the calculated amount.
    public TransportRequest calculateFuel(Long requestId, Double litres, String reason) {

        TransportRequest request = repository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.VEHICLE_ALLOCATED) {
            throw new IllegalStateException("Fuel can only be calculated once a vehicle is allocated. "
                    + request.getStatus().alreadyMovedOn());
        }

        Double suggested = fuelCalculationService.calculateFuel(request.getDistanceKm());
        double load = litres == null ? suggested : litres;
        // a tenth of a litre over is rounding, not extra
        boolean over = load > suggested + 0.05;
        if (over && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException(String.format(
                    "%.1f L is more than the %.1f L calculated. Explain why the extra fuel is needed.", load, suggested));
        }
        request.setFuelSuggestedLitres(suggested);
        request.setFuelRequiredLitres(load);
        request.setFuelOverReason(over ? reason.trim() : null);
        request.setStatus(RequestStatus.FUEL_CALCULATED);

        repository.save(request);

        notificationService.notifyFuelRequestSubmitted(request);
        notificationService.notifyUser(request.getDriverId(), "Fuel set - Request #" + requestId,
                (load <= 0 ? "No fuel is needed for your trip, the vehicle has enough." : "Fuel for your trip is set at " + load + " litres.")
                        + "\nIt is now waiting for HR & Admin approval.\n",
                NotificationService.requestLink(requestId));

        return request;
    }

    // the vehicle admin got the fuel wrong: back to "waiting for fuel" so it can be calculated again
    public TransportRequest undoFuelCalculation(Long requestId) {
        TransportRequest request = repository.findById(requestId).orElseThrow();
        if (request.getStatus() != RequestStatus.FUEL_CALCULATED) {
            throw new IllegalStateException("Fuel can only be recalculated before HR & Admin approve the trip. "
                    + request.getStatus().alreadyMovedOn());
        }
        clearFuelCalculation(request);
        request.setStatus(RequestStatus.VEHICLE_ALLOCATED);
        notificationService.notifyUser(request.getDriverId(), "Fuel being recalculated - Request #" + requestId,
                "The vehicle admin is correcting the fuel for your trip to " + request.getDestination() + ". You'll be told the new amount.\n",
                NotificationService.requestLink(requestId));
        return repository.save(request);
    }

    // back to what create() set: the distance's litres, nothing chosen by the vehicle admin
    public static void clearFuelCalculation(TransportRequest request) {
        if (request.getFuelSuggestedLitres() != null) {
            request.setFuelRequiredLitres(request.getFuelSuggestedLitres());
        }
        request.setFuelSuggestedLitres(null);
        request.setFuelOverReason(null);
    }
}
