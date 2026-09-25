package zw.co.petrotrade.workflow.approval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;

import java.util.Map;

@Service
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

    public Approval decide(
            Long requestId,
            ApprovalLevel level,
            boolean approved,
            String approver,
            String comments) {

        if (level == ApprovalLevel.FUEL) {
            throw new IllegalArgumentException(
                    "Fuel approval is handled by FuelApprovalService");
        }

        TransportRequest request =
                requestRepository.findById(requestId)
                        .orElseThrow();

        RequestStatus required = REQUIRED_STATUS.get(level);
        if (request.getStatus() != required) {
            throw new IllegalStateException(
                    "Request " + requestId + " is not awaiting " + level
                            + " approval (current status: " + request.getStatus() + ")");
        }

        request.setStatus(approved ? APPROVED_STATUS.get(level) : REJECTED_STATUS.get(level));
        requestRepository.save(request);

        return approvalRepository.save(Approval.record(
                ApprovalSubject.TRANSPORT_REQUEST, requestId, level, approved, approver, comments));
    }
}
