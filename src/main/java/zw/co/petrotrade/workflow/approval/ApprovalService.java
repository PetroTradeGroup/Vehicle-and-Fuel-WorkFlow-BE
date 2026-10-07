package zw.co.petrotrade.workflow.approval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApprovalService {

    private static final Map<ApprovalLevel, RequestStatus> REQUIRED_STATUS = Map.of(
            ApprovalLevel.HOD, RequestStatus.PENDING_HOD,
            ApprovalLevel.HR_ADMIN, RequestStatus.APPROVED_HOD
    );

    private static final Map<ApprovalLevel, RequestStatus> APPROVED_STATUS = Map.of(
            ApprovalLevel.HOD, RequestStatus.APPROVED_HOD,
            ApprovalLevel.HR_ADMIN, RequestStatus.APPROVED_HR_ADMIN
    );

    private static final Map<ApprovalLevel, RequestStatus> REJECTED_STATUS = Map.of(
            ApprovalLevel.HOD, RequestStatus.REJECTED_HOD,
            ApprovalLevel.HR_ADMIN, RequestStatus.REJECTED_HR_ADMIN
    );

    private final ApprovalRepository approvalRepository;
    private final TransportRequestRepository requestRepository;
    private final CurrentUser currentUser;

    public Approval decide(
            Long requestId,
            ApprovalLevel level,
            boolean approved,
            String comments) {

        if (level == ApprovalLevel.FUEL) {
            throw new IllegalArgumentException(
                    "Fuel approval is handled by FuelApprovalService");
        }

        TransportRequest request =
                requestRepository.findById(requestId)
                        .orElseThrow();

        User approver = currentUser.get();
        if (level == ApprovalLevel.HOD) {
            currentUser.require(Role.HOD);
            // a head of department only decides for their own department
            if (approver.getDepartment() == null || !approver.getDepartment().getId().equals(request.getDepartmentId())) {
                throw new AccessDeniedException("Only the head of the " + request.getDepartment()
                        + " department can approve this request");
            }
        } else {
            currentUser.require(Role.HR_ADMIN_MANAGER);
        }

        RequestStatus required = REQUIRED_STATUS.get(level);
        if (request.getStatus() != required) {
            throw new IllegalStateException("This request isn't at the "
                    + (level == ApprovalLevel.HOD ? "department head" : "HR & Admin") + " step. "
                    + request.getStatus().alreadyMovedOn());
        }

        request.setStatus(approved ? APPROVED_STATUS.get(level) : REJECTED_STATUS.get(level));
        log.info("Approval request {} has been decided", request);
        TransportRequest px=     requestRepository.save(request);

        return approvalRepository.save(Approval.record(
                ApprovalSubject.TRANSPORT_REQUEST, requestId, level, approved, approver.getFullName(), comments));
    }
}
