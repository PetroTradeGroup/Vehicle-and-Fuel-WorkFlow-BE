package zw.co.petrotrade.workflow;

import org.junit.jupiter.api.Test;
import zw.co.petrotrade.workflow.approval.Approval;
import zw.co.petrotrade.workflow.approval.ApprovalLevel;
import zw.co.petrotrade.workflow.approval.ApprovalRepository;
import zw.co.petrotrade.workflow.approval.ApprovalService;
import zw.co.petrotrade.workflow.fuel.FuelApprovalService;
import zw.co.petrotrade.workflow.fuel.FuelCard;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.fuel.FuelTransaction;
import zw.co.petrotrade.workflow.fuel.FuelTransactionRepository;
import zw.co.petrotrade.workflow.notification.NotificationService;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TransportRequestRepository;
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

        FuelApprovalService finalApproval = new FuelApprovalService(requests, transactions, approvals, allocations, null, vehicles, mock(NotificationService.class));
        Approval decision = finalApproval.decide(1L, false, false, "HR Manager", "Not needed");

        assertEquals(ApprovalLevel.HR_ADMIN, decision.getLevel());
        assertEquals(RequestStatus.REJECTED_HR_ADMIN, request.getStatus());
        assertEquals(VehicleStatus.AVAILABLE, car.getStatus());
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
