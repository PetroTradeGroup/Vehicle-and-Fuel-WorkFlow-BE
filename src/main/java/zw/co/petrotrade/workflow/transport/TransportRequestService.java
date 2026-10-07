package zw.co.petrotrade.workflow.transport;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalSubject;
import zw.co.petrotrade.workflow.fuel.FuelCalculationService;
import zw.co.petrotrade.workflow.fuel.NotificationService;
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
        if (skipReason != null) {
            approvalRepository.save(Approval.record(ApprovalSubject.TRANSPORT_REQUEST, saved.getId(), ApprovalLevel.HOD,
                    true, "Automatic", skipReason));
        }
        return saved;
    }

    // The department head step is skipped when nobody else could approve it; HR & Admin still approve every request
    private String hodSkipReason(User driver) {
        if (driver.getDepartment() == null) {
            return driver.getFullName() + " has no department, so this went straight to HR & Admin.";
        }
        String department = driver.getDepartment().getName();
        if (driver.getRole() == Role.HOD) {
            return "Requested by the head of " + department + ", so this went straight to HR & Admin.";
        }
        if (userRepository.findByRoleAndDepartment_Id(Role.HOD, driver.getDepartment().getId()).isEmpty()) {
            return department + " has no head of department, so this went straight to HR & Admin.";
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
