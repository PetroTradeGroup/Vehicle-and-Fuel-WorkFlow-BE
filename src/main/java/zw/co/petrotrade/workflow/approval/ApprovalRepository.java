package zw.co.petrotrade.workflow.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findBySubjectAndRequestIdOrderByApprovalDateAsc(ApprovalSubject subject, Long requestId);

    List<Approval> findBySubjectAndRequestIdIn(ApprovalSubject subject, Collection<Long> requestIds);
}
