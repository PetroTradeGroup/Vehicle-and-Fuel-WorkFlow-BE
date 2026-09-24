package zw.co.petrotrade.workflow.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByRequestIdOrderByApprovalDateAsc(Long requestId);
}