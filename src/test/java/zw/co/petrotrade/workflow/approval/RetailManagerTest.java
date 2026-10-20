package zw.co.petrotrade.workflow.approval;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import zw.co.petrotrade.workflow.department.Department;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// the retail manager is also head of their department: they give its department head approval
class RetailManagerTest {

    @Test
    void retailManagerApprovesTheirOwnDepartmentsRequestsOnly() {
        Department retail = new Department();
        retail.setId(3L);
        retail.setName("Retail");
        User manager = new User();
        manager.setFullName("Retail Manager");
        manager.setRole(Role.RETAIL_MANAGER);
        manager.setDepartment(retail);

        CurrentUser currentUser = mock(CurrentUser.class, CALLS_REAL_METHODS);
        doReturn(List.of(Role.RETAIL_MANAGER)).when(currentUser).roles();
        doReturn(manager).when(currentUser).get();

        TransportRequestRepository requests = mock(TransportRequestRepository.class);
        ApprovalRepository approvals = mock(ApprovalRepository.class);
        when(approvals.save(any())).thenAnswer(i -> i.getArgument(0));
        TransportRequest own = request(1L, 3L, requests);
        request(2L, 4L, requests);
        ApprovalService service = new ApprovalService(approvals, requests, currentUser, mock(NotificationService.class));

        service.decide(1L, ApprovalLevel.HOD, true, null);
        assertEquals(RequestStatus.APPROVED_HOD, own.getStatus());
        assertThrows(AccessDeniedException.class, () -> service.decide(2L, ApprovalLevel.HOD, true, null));
    }

    private static TransportRequest request(Long id, Long departmentId, TransportRequestRepository requests) {
        TransportRequest request = new TransportRequest();
        request.setId(id);
        request.setDepartmentId(departmentId);
        request.setStatus(RequestStatus.PENDING_HOD);
        when(requests.findById(id)).thenReturn(Optional.of(request));
        return request;
    }
}
