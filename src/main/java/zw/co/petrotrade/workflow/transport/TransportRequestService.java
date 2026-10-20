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

    public TransportRequest calculateFuel(Long requestId) {

        TransportRequest request = repository.findById(requestId).orElseThrow();

        if (request.getStatus() != RequestStatus.VEHICLE_ALLOCATED) {
            throw new IllegalStateException("Fuel can only be calculated once a vehicle is allocated. "
                    + request.getStatus().alreadyMovedOn());
        }

        request.setFuelRequiredLitres(
                fuelCalculationService.calculateFuel(request.getDistanceKm()));
        request.setStatus(RequestStatus.FUEL_CALCULATED);

        repository.save(request);

        notificationService.notifyFuelRequestSubmitted(request);

        return request;
    }
}
