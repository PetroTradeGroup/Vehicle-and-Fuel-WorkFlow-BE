package zw.co.petrotrade.workflow.approval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;

// The department head's decision. HR & Admin decide once, at the end, on the trip and its fuel
// together: that goes through FuelApprovalService.
@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final TransportRequestRepository requestRepository;
    private final CurrentUser currentUser;
    private final NotificationService notifications;

    public Approval decide(
            Long requestId,
            ApprovalLevel level,
            boolean approved,
            String comments) {

        if (level != ApprovalLevel.HOD) {
            throw new IllegalArgumentException(
                    "HR & Admin approve the trip and its fuel together once fuel is calculated. Use the final approval.");
        }

        TransportRequest request =
                requestRepository.findById(requestId)
                        .orElseThrow();

        User approver = currentUser.get();
        currentUser.require(Role.HOD, Role.RETAIL_MANAGER);
        // a head of department only decides for their own department
        if (approver.getDepartment() == null || !approver.getDepartment().getId().equals(request.getDepartmentId())) {
            throw new AccessDeniedException("Only the head of the " + request.getDepartment()
                    + " department can approve this request");
        }

        if (request.getStatus() != RequestStatus.PENDING_HOD) {
            throw new IllegalStateException("This request isn't at the department head step. "
                    + request.getStatus().alreadyMovedOn());
        }

        request.setStatus(approved ? RequestStatus.APPROVED_HOD : RequestStatus.REJECTED_HOD);
        log.info("Request {} {} by the department head", requestId, approved ? "approved" : "rejected");
        requestRepository.save(request);
        String note = comments == null || comments.isBlank() ? "" : "Comment from " + approver.getFullName() + ": " + comments.trim() + "\n";
        notifications.notifyUser(request.getDriverId(), "Request #" + requestId + (approved ? " approved by your head of department" : " rejected by your head of department"),
                "Your trip to " + request.getDestination() + (approved ? " was approved by " : " was rejected by ") + approver.getFullName() + ".\n"
                        + (approved ? "A vehicle admin allocates the vehicle next.\n" : "") + note,
                NotificationService.requestLink(requestId));
        if (approved) {
            notifications.notifyVehicleAdmins("Vehicle needed - Request #" + requestId,
                    request.getDriverName() + " (" + request.getDepartment() + "): " + request.getStartingPoint() + " to " + request.getDestination()
                            + ", from " + request.getRequiredFrom() + ".\nApproved by " + approver.getFullName() + ". Allocate a vehicle.\n",
                    NotificationService.requestLink(requestId));
        }

        return approvalRepository.save(Approval.record(
                ApprovalSubject.TRANSPORT_REQUEST, requestId, level, approved, approver.getFullName(), comments));
    }
}
