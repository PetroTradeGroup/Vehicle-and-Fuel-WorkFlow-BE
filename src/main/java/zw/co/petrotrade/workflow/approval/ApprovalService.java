package zw.co.petrotrade.workflow.approval;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.request.RequestStatus;
import zw.co.petrotrade.workflow.request.TransportRequest;
import zw.co.petrotrade.workflow.request.TransportRequestRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final TransportRequestRepository requestRepository;

    public Approval approve(
            Long requestId,
            ApprovalLevel level,
            String approver,
            String comments) {

        TransportRequest request =
                requestRepository.findById(requestId)
                        .orElseThrow();

        Approval approval = new Approval();

        approval.setRequestId(requestId);
        approval.setApprover(approver);
        approval.setLevel(level);
        approval.setComments(comments);
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setApprovalDate(LocalDateTime.now());

        switch (level) {

            case HOD ->
                    request.setStatus(
                            RequestStatus.APPROVED_HOD);

            case HR ->
                    request.setStatus(
                            RequestStatus.APPROVED_HR);

            case ADMIN ->
                    request.setStatus(
                            RequestStatus.APPROVED_ADMIN);
        }

        requestRepository.save(request);

        return approvalRepository.save(approval);
    }
}