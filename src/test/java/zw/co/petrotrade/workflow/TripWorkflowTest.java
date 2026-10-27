package zw.co.petrotrade.workflow;

import org.junit.jupiter.api.Test;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalService;
import zw.co.petrotrade.workflow.fuel.FuelApprovalService;
import zw.co.petrotrade.workflow.fuel.FuelCalculationService;
import zw.co.petrotrade.workflow.fuel.FuelCard;
import zw.co.petrotrade.workflow.fuel.CardHolderType;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.fuel.FuelCardService;
import zw.co.petrotrade.workflow.fuel.FuelTransaction;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;
import zw.co.petrotrade.workflow.transport.TransportRequestService;
import zw.co.petrotrade.workflow.transport.TripType;
import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocationServiceImp;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The trip rules: local/external, odometer readings, and HR & Admin approving once at the end
class TripWorkflowTest {

    private final TransportRequestRepository requests = mock(TransportRequestRepository.class);
    private final VehicleRepository vehicles = mock(VehicleRepository.class);
    private final VehicleAllocationRepository allocations = mock(VehicleAllocationRepository.class);
    private final FuelTransactionRepository transactions = mock(FuelTransactionRepository.class);
    private final FuelCardRepository cards = mock(FuelCardRepository.class);
    private final ApprovalRepository approvals = mock(ApprovalRepository.class);

    private final VehicleAllocationServiceImp allocationService =
            new VehicleAllocationServiceImp(allocations, vehicles, requests, transactions, cards, mock(NotificationService.class));

    {
        when(allocations.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvals.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void tripsOf50kmOrMoreAreExternal() {
        assertEquals(TripType.LOCAL, TripType.of(49.9));
        assertEquals(TripType.EXTERNAL, TripType.of(50.0));
        assertEquals(TripType.EXTERNAL, TripType.of(300.0));
        assertEquals(TripType.LOCAL, TripType.of(null));
    }

    @Test
    void allocationRightAfterHodStartsFromTheLastReading() {
        request(RequestStatus.APPROVED_HOD);
        Vehicle car = vehicle(12_000.0);

        VehicleAllocation allocation = allocationService.allocate(1L, 7L, "Admin", null);

        assertEquals(12_000.0, allocation.getOdometerOutKm());
        assertEquals(VehicleStatus.ALLOCATED, car.getStatus());
    }

    @Test
    void legacyHrApprovedRequestsCanStillBeAllocated() {
        TransportRequest request = request(RequestStatus.APPROVED_HR_ADMIN);
        vehicle(100.0);

        allocationService.allocate(1L, 7L, "Admin", 150.0);

        assertEquals(RequestStatus.VEHICLE_ALLOCATED, request.getStatus());
    }

    @Test
    void aWrongAllocationCanBeUndoneUntilHrAndAdminApprove() {
        TransportRequest request = request(RequestStatus.APPROVED_HOD);
        Vehicle car = vehicle(1_000.0);
        VehicleAllocation allocation = allocationService.allocate(1L, 7L, "Admin", 1_500.0);
        when(allocations.findByRequestId(1L)).thenReturn(Optional.of(allocation));
        request.setStatus(RequestStatus.FUEL_CALCULATED);
        request.setFuelSuggestedLitres(30.0);
        request.setFuelRequiredLitres(40.0);
        request.setFuelOverReason("Extra");

        allocationService.undoAllocation(1L);

        assertEquals(RequestStatus.APPROVED_HOD, request.getStatus());
        assertEquals(VehicleStatus.AVAILABLE, car.getStatus());
        assertEquals(1_000.0, car.getOdometerKm());
        assertEquals(30.0, request.getFuelRequiredLitres());
        assertNull(request.getFuelOverReason());
        verify(allocations).delete(allocation);

        request.setStatus(RequestStatus.FUEL_APPROVED);
        assertThrows(IllegalStateException.class, () -> allocationService.undoAllocation(1L));
    }

    @Test
    void allocationNeedsAReadingAndItCantGoBackwards() {
        request(RequestStatus.APPROVED_HOD);
        vehicle(null);
        assertThrows(IllegalArgumentException.class, () -> allocationService.allocate(1L, 7L, "Admin", null));

        vehicle(500.0);
        assertThrows(IllegalArgumentException.class, () -> allocationService.allocate(1L, 7L, "Admin", 499.0));
    }

    @Test
    void returnWorksOutDistanceAndRollsTheReadingForward() {
        request(RequestStatus.FUEL_APPROVED);
        Vehicle car = vehicle(1_000.0);
        car.setStatus(VehicleStatus.ALLOCATED);
        VehicleAllocation out = new VehicleAllocation();
        out.setVehicleId(7L);
        out.setOdometerOutKm(1_000.0);
        when(allocations.findByRequestId(1L)).thenReturn(Optional.of(out));
        fuelCardWith(40.0);

        assertThrows(IllegalArgumentException.class, () -> allocationService.returnVehicle(1L, "Admin", 10, 999));

        VehicleAllocation back = allocationService.returnVehicle(1L, "Admin", 10, 1_120);

        assertEquals(120.0, back.getDistanceTravelledKm());
        assertEquals(1_120.0, back.getOdometerInKm());
        assertEquals(1_120.0, car.getOdometerKm());
        assertEquals(VehicleStatus.AVAILABLE, car.getStatus());
    }

    @Test
    void hrAndAdminOnlyDecideAtTheEnd() {
        ApprovalService hodApprovals = new ApprovalService(approvals, requests, null, mock(NotificationService.class));
        assertThrows(IllegalArgumentException.class,
                () -> hodApprovals.decide(1L, ApprovalLevel.HR_ADMIN, true, null));

        TransportRequest request = request(RequestStatus.FUEL_CALCULATED);
        Vehicle car = vehicle(1_000.0);
        car.setStatus(VehicleStatus.ALLOCATED);
        VehicleAllocation allocation = new VehicleAllocation();
        allocation.setVehicleId(7L);
        when(allocations.findByRequestId(1L)).thenReturn(Optional.of(allocation));

        FuelApprovalService finalApproval = new FuelApprovalService(requests, transactions, approvals, allocations, null, cards, vehicles, mock(NotificationService.class));
        Approval decision = finalApproval.decide(1L, false, false, "HR Manager", "Not needed");

        assertEquals(ApprovalLevel.HR_ADMIN, decision.getLevel());
        assertEquals(RequestStatus.REJECTED_HR_ADMIN, request.getStatus());
        assertEquals(VehicleStatus.AVAILABLE, car.getStatus());
    }

    @Test
    void aTripOnTheFuelInTheTankLoadsNothingAndStillReturns() {
        TransportRequest request = request(RequestStatus.FUEL_CALCULATED);
        request.setFuelRequiredLitres(0.0);
        VehicleAllocation allocation = new VehicleAllocation();
        allocation.setVehicleId(7L);
        allocation.setOdometerOutKm(1_000.0);
        when(allocations.findByRequestId(1L)).thenReturn(Optional.of(allocation));
        when(transactions.findFirstByRequestIdOrderByIdDesc(1L)).thenReturn(Optional.empty());

        // no card service: crediting a card would fail the test
        new FuelApprovalService(requests, transactions, approvals, allocations, null, cards, vehicles, mock(NotificationService.class))
                .decide(1L, true, false, "HR Manager", null);
        assertEquals(RequestStatus.FUEL_APPROVED, request.getStatus());
        verify(transactions, never()).save(any());

        vehicle(1_000.0).setStatus(VehicleStatus.ALLOCATED);
        VehicleAllocation back = allocationService.returnVehicle(1L, "Admin", 8, 1_050);
        assertEquals(8.0, back.getFuelUsedLitres());
        assertEquals(RequestStatus.COMPLETED, request.getStatus());
    }

    @Test
    void theCardOnlyGoesUpOnceItHasLoadedTheFuel() {
        TransportRequest request = request(RequestStatus.FUEL_CALCULATED);
        request.setFuelRequiredLitres(30.0);
        VehicleAllocation allocation = new VehicleAllocation();
        allocation.setVehicleId(7L);
        when(allocations.findByRequestId(1L)).thenReturn(Optional.of(allocation));
        FuelCard card = new FuelCard();
        card.setId(3L);
        card.setCardNumber("7001");
        card.setBalanceLitres(5.0);
        card.setActive(true);
        FuelCardService cardService = mock(FuelCardService.class);
        when(cardService.findActiveCard(CardHolderType.VEHICLE, 7L)).thenReturn(card);
        when(cardService.credit(card, 30.0)).thenAnswer(i -> {
            card.setBalanceLitres(card.getBalanceLitres() + 30.0);
            return card;
        });
        when(cards.findById(3L)).thenReturn(Optional.of(card));
        FuelTransaction[] saved = new FuelTransaction[1];
        when(transactions.save(any())).thenAnswer(i -> saved[0] = i.getArgument(0));

        FuelApprovalService service = new FuelApprovalService(requests, transactions, approvals, allocations, cardService, cards, vehicles, mock(NotificationService.class));
        service.decide(1L, true, false, "HR Manager", null);
        assertEquals(5.0, card.getBalanceLitres());
        assertEquals(Boolean.FALSE, saved[0].getLoaded());

        // the vehicle can't come back off a card the fuel was never put on
        saved[0].setId(9L);
        when(transactions.findFirstByRequestIdOrderByIdDesc(1L)).thenReturn(Optional.of(saved[0]));
        when(transactions.findById(9L)).thenReturn(Optional.of(saved[0]));
        assertThrows(IllegalStateException.class, () -> allocationService.returnVehicle(1L, "Admin", 10, 100));

        service.markLoaded(9L, "IT Admin");
        assertEquals(35.0, card.getBalanceLitres());
        assertEquals("IT Admin", saved[0].getLoadedBy());
        assertThrows(IllegalStateException.class, () -> service.markLoaded(9L, "IT Admin"));
    }

    @Test
    void theVehicleAdminSetsTheFuelAndExtraNeedsAReason() {
        TransportRequestService service = new TransportRequestService(requests, null, new FuelCalculationService(), mock(NotificationService.class), approvals);

        // external (300 km = 30 L): the card already holds some, so only a top-up is loaded
        TransportRequest external = request(RequestStatus.VEHICLE_ALLOCATED);
        external.setDistanceKm(300.0);
        assertEquals(10.0, service.calculateFuel(1L, 10.0, null).getFuelRequiredLitres());

        // local (40 km = 4 L): less is fine, more needs a reason
        TransportRequest local = request(RequestStatus.VEHICLE_ALLOCATED);
        local.setDistanceKm(40.0);
        assertEquals(0.0, service.calculateFuel(1L, 0.0, null).getFuelRequiredLitres());
        local.setStatus(RequestStatus.VEHICLE_ALLOCATED);
        assertThrows(IllegalArgumentException.class, () -> service.calculateFuel(1L, 10.0, " "));
        TransportRequest extra = service.calculateFuel(1L, 10.0, "Generator run at the site");
        assertEquals(10.0, extra.getFuelRequiredLitres());
        assertEquals(4.0, extra.getFuelSuggestedLitres());
        assertEquals("Generator run at the site", extra.getFuelOverReason());
    }

    private TransportRequest request(RequestStatus status) {
        TransportRequest request = new TransportRequest();
        request.setId(1L);
        request.setStatus(status);
        when(requests.findById(1L)).thenReturn(Optional.of(request));
        return request;
    }

    private Vehicle vehicle(Double odometerKm) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(7L);
        vehicle.setRegistrationNumber("AZW 6789");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setOdometerKm(odometerKm);
        when(vehicles.findById(7L)).thenReturn(Optional.of(vehicle));
        return vehicle;
    }

    private void fuelCardWith(double litres) {
        FuelCard card = new FuelCard();
        card.setId(3L);
        card.setBalanceLitres(litres);
        FuelTransaction transaction = new FuelTransaction();
        transaction.setFuelCardId(3L);
        when(transactions.findFirstByRequestIdOrderByIdDesc(1L)).thenReturn(Optional.of(transaction));
        when(cards.findById(3L)).thenReturn(Optional.of(card));
    }
}
