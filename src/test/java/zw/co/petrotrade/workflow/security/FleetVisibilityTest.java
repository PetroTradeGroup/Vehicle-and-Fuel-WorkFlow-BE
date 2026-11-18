package zw.co.petrotrade.workflow.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import zw.co.petrotrade.workflow.fuel.CardHolderType;
import zw.co.petrotrade.workflow.fuel.FuelApprovalService;
import zw.co.petrotrade.workflow.fuel.FuelCard;
import zw.co.petrotrade.workflow.fuel.FuelCardController;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.fuel.FuelCardService;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.vehicle.ServiceStatus;
import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleController;
import zw.co.petrotrade.workflow.vehicle.VehicleServiceImp;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Fuel card balances and service due data are for HR & Admin, vehicle admins and system admins only
class FleetVisibilityTest {

    private final FuelCardRepository cards = mock(FuelCardRepository.class);
    private final VehicleServiceImp vehicles = mock(VehicleServiceImp.class);

    FleetVisibilityTest() {
        when(cards.findAll()).thenReturn(List.of(card(1L, CardHolderType.VEHICLE), card(2L, CardHolderType.STATION)));
        when(cards.findById(1L)).thenReturn(Optional.of(card(1L, CardHolderType.VEHICLE)));
        Vehicle car = new Vehicle();
        car.setId(7L);
        car.setRegistrationNumber("AZW 6789");
        car.setOdometerKm(15_000.0);
        car.setLastServiceOdometerKm(10_000.0);
        when(vehicles.findAll()).thenReturn(List.of(car));
        when(vehicles.findById(7L)).thenReturn(car);
    }

    @Test
    void driversAndHodsCannotSeeFuelCards() {
        for (Role role : List.of(Role.DRIVER, Role.HOD)) {
            FuelCardController controller = cardsAs(role);
            assertThrows(AccessDeniedException.class, controller::list, role.name());
            assertThrows(AccessDeniedException.class, () -> controller.get(1L), role.name());
        }
    }

    @Test
    void fleetRolesSeeVehicleCardsAndStationRolesOnlyStationCards() {
        for (Role role : List.of(Role.HR_ADMIN_MANAGER, Role.VEHICLE_ADMIN, Role.SYSTEM_ADMIN)) {
            assertTrue(cardsAs(role).list().stream().anyMatch(c -> c.holderType() == CardHolderType.VEHICLE), role.name());
        }
        FuelCardController rep = cardsAs(Role.REGIONAL_REP);
        assertTrue(rep.list().stream().allMatch(c -> c.holderType() == CardHolderType.STATION));
        assertThrows(AccessDeniedException.class, () -> rep.get(1L));
    }

    @Test
    void serviceDataIsHiddenFromEveryoneButTheFleetRoles() {
        VehicleResponse driverView = vehiclesAs(Role.DRIVER).get(7L);
        assertEquals("AZW 6789", driverView.registrationNumber());
        assertNull(driverView.serviceStatus());
        assertNull(driverView.kmToService());
        assertNull(driverView.lastServiceOdometerKm());
        assertNull(vehiclesAs(Role.HOD).list(null, null).get(0).serviceStatus());

        for (Role role : List.of(Role.HR_ADMIN_MANAGER, Role.VEHICLE_ADMIN, Role.SYSTEM_ADMIN)) {
            assertEquals(ServiceStatus.DUE, vehiclesAs(role).get(7L).serviceStatus(), role.name());
        }
    }

    private FuelCardController cardsAs(Role role) {
        return new FuelCardController(cards, mock(FuelCardService.class), mock(FuelApprovalService.class), user(role));
    }

    private VehicleController vehiclesAs(Role role) {
        return new VehicleController(vehicles, user(role));
    }

    private static CurrentUser user(Role role) {
        CurrentUser currentUser = mock(CurrentUser.class, CALLS_REAL_METHODS);
        doReturn(List.of(role)).when(currentUser).roles();
        return currentUser;
    }

    private static FuelCard card(Long id, CardHolderType type) {
        FuelCard card = new FuelCard();
        card.setId(id);
        card.setCardNumber("CARD-" + id);
        card.setHolderType(type);
        card.setHolderId(id);
        card.setBalanceLitres(50.0);
        card.setActive(true);
        return card;
    }
}
