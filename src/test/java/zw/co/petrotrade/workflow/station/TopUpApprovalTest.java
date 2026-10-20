package zw.co.petrotrade.workflow.station;

import org.junit.jupiter.api.Test;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.fuel.FuelCardService;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.notification.NotificationService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// a regional rep or the retail manager approves first; only HR & Admin's approval credits the card
class TopUpApprovalTest {

    @Test
    void firstApprovalPassesItToHrWithoutCreditingTheCard() {
        StationTopUpRepository repository = mock(StationTopUpRepository.class);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        StationTopUp topUp = new StationTopUp();
        topUp.setStatus(TopUpStatus.PENDING_FIRST_APPROVAL);
        when(repository.findById(1L)).thenReturn(Optional.of(topUp));
        FuelCardService cards = mock(FuelCardService.class);
        StationTopUpService service = new StationTopUpService(repository, mock(StationRepository.class), cards,
                mock(FuelTransactionRepository.class), mock(ApprovalRepository.class), mock(NotificationService.class));

        // HR can't skip the first approval
        assertThrows(IllegalStateException.class, () -> service.decide(1L, TopUpStatus.PENDING_APPROVAL, true, "HR", null));

        service.decide(1L, TopUpStatus.PENDING_FIRST_APPROVAL, true, "Regional rep", null);
        assertEquals(TopUpStatus.PENDING_APPROVAL, topUp.getStatus());
        verifyNoInteractions(cards);

        // a second click on the first approval is stale
        assertThrows(IllegalStateException.class, () -> service.decide(1L, TopUpStatus.PENDING_FIRST_APPROVAL, true, "Retail", null));

        service.decide(1L, TopUpStatus.PENDING_APPROVAL, false, "HR", "Not needed");
        assertEquals(TopUpStatus.REJECTED, topUp.getStatus());
    }
}
