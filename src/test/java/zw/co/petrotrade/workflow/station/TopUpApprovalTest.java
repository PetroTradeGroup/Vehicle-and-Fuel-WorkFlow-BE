package zw.co.petrotrade.workflow.station;

import org.junit.jupiter.api.Test;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.fuel.FuelCardService;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.station.dto.StationTopUpRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// a regional rep or the retail manager approves first, then HR & Admin; the card is only credited once IT loads it
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
                mock(FuelTransactionRepository.class), mock(ApprovalRepository.class), mock(NotificationService.class), null);

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

    @Test
    void generatorHoursCantGoBackwardsAndGrassCuttersGetTenLitres() {
        Station station = new Station();
        station.setName("Msasa");
        station.setGeneratorHours(1200.0);
        StationRepository stations = mock(StationRepository.class);
        when(stations.findById(5L)).thenReturn(Optional.of(station));
        StationTopUpService service = new StationTopUpService(mock(StationTopUpRepository.class), stations, mock(FuelCardService.class),
                mock(FuelTransactionRepository.class), mock(ApprovalRepository.class), mock(NotificationService.class), null);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(new StationTopUpRequest(5L, 1L, 50.0, TopUpPurpose.GENERATOR, 1100.0, "Low"), "Station admin"));
        assertEquals(1200.0, station.getGeneratorHours());
        // the generator needs its hours
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new StationTopUpRequest(5L, 1L, 50.0, TopUpPurpose.GENERATOR, null, "Low"), "Station admin"));

        IllegalArgumentException tooMuch = assertThrows(IllegalArgumentException.class,
                () -> service.create(new StationTopUpRequest(5L, 1L, 10.5, TopUpPurpose.GRASS_CUTTER, null, "Grass"), "Station admin"));
        assertTrue(tooMuch.getMessage().contains("10 litres"));
    }
}
