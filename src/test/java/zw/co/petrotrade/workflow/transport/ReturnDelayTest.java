package zw.co.petrotrade.workflow.transport;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalStatus;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Overdue returns, extension requests and late explanations
class ReturnDelayTest {

    private static final LocalDate DUE = LocalDate.of(2026, 10, 10);

    private final TransportRequestRepository requests = mock(TransportRequestRepository.class);
    private final VehicleAllocationRepository allocations = mock(VehicleAllocationRepository.class);
    private final ApprovalRepository approvals = mock(ApprovalRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final ReturnDelayService delays =
            new ReturnDelayService(requests, allocations, mock(VehicleRepository.class), approvals, notifications);

    @Test
    void overdueOnlyOnceTheReturnDateHasPassedOnATripThatsOut() {
        TransportRequest trip = trip(RequestStatus.FUEL_APPROVED);
        assertFalse(ReturnDelayService.overdue(trip, DUE));
        assertTrue(ReturnDelayService.overdue(trip, DUE.plusDays(1)));
        assertFalse(ReturnDelayService.overdue(trip(RequestStatus.COMPLETED), DUE.plusDays(5)));
    }

    @Test
    void approvedExtensionMovesTheReturnDateAndIsRecorded() {
        TransportRequest trip = trip(RequestStatus.FUEL_APPROVED);

        assertThrows(IllegalArgumentException.class, () -> delays.requestExtension(1L, DUE, "Road closed"));
        assertThrows(IllegalArgumentException.class, () -> delays.requestExtension(1L, DUE.plusDays(2), " "));

        delays.requestExtension(1L, DUE.plusDays(2), "Road closed");
        assertEquals(DUE.plusDays(2), trip.getExtensionRequestedTo());
        verify(notifications).notifyVehicleAdmins(startsWith("Extension requested"), contains("Road closed"), eq("/requests/1"));

        delays.decideExtension(1L, true, "Vee Admin", null);
        assertEquals(DUE.plusDays(2), trip.getRequiredTo());
        assertNull(trip.getExtensionRequestedTo());
        ArgumentCaptor<Approval> saved = ArgumentCaptor.forClass(Approval.class);
        verify(approvals).save(saved.capture());
        assertEquals(ApprovalLevel.EXTENSION, saved.getValue().getLevel());
        assertEquals(ApprovalStatus.APPROVED, saved.getValue().getStatus());

        assertThrows(IllegalStateException.class, () -> delays.decideExtension(1L, true, "Vee Admin", null));
    }

    @Test
    void rejectedExtensionKeepsTheDateAndAnExplanationNeedsNoDate() {
        TransportRequest trip = trip(RequestStatus.FUEL_APPROVED);
        delays.requestExtension(1L, DUE.plusDays(3), "Client meeting moved");
        delays.decideExtension(1L, false, "Vee Admin", "Bring it back");
        assertEquals(DUE, trip.getRequiredTo());

        delays.requestExtension(1L, null, "Puncture on the way back");
        assertEquals("Puncture on the way back", trip.getDelayReason());
        assertNull(trip.getExtensionRequestedTo());
        verify(notifications).notifyVehicleAdmins(startsWith("Late return explained"), anyString(), anyString());
    }

    @Test
    void dailyAlertListsOverdueVehiclesForTheAdmins() {
        TransportRequest trip = trip(RequestStatus.FUEL_APPROVED);
        when(requests.findByStatusAndRequiredToBeforeOrderByRequiredTo(eq(RequestStatus.FUEL_APPROVED), any())).thenReturn(List.of(trip));

        delays.alertOverdueReturns();

        verify(notifications).notifyVehicleAdmins(eq("1 vehicle is past their return date"), contains("Jane Moyo (request #1)"), anyString());
        verify(notifications).notifyUser(eq(3L), startsWith("Vehicle overdue"), anyString(), eq("/requests/1"));
    }

    private TransportRequest trip(RequestStatus status) {
        TransportRequest trip = new TransportRequest();
        trip.setId(1L);
        trip.setDriverId(3L);
        trip.setDriverName("Jane Moyo");
        trip.setDestination("Mutare");
        trip.setRequiredTo(DUE);
        trip.setStatus(status);
        when(requests.findById(1L)).thenReturn(Optional.of(trip));
        return trip;
    }
}
