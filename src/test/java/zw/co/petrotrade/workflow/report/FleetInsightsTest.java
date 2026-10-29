package zw.co.petrotrade.workflow.report;

import org.junit.jupiter.api.Test;
import zw.co.petrotrade.workflow.fuel.FuelCardRepository;
import zw.co.petrotrade.workflow.fuel.FuelTypeRepository;
import zw.co.petrotrade.workflow.transport.RequestStatus;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.vehicle.ServiceStatus;
import zw.co.petrotrade.workflow.vehicle.Vehicle;
import zw.co.petrotrade.workflow.vehicle.VehicleAllocation;
import zw.co.petrotrade.workflow.vehicle.VehicleRepository;
import zw.co.petrotrade.workflow.vehicle.VehicleServiceImp;
import zw.co.petrotrade.workflow.vehicle.dto.VehicleServiceRequest;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Service every 5000 km, and the top fuel users / most used vehicles on the trip report
class FleetInsightsTest {

    @Test
    void serviceIsDueEvery5000kmFromTheLastService() {
        assertEquals(ServiceStatus.NOT_RECORDED, ServiceStatus.of(vehicle(12_000.0, null)));
        assertEquals(ServiceStatus.OK, ServiceStatus.of(vehicle(14_000.0, 10_000.0)));
        assertEquals(ServiceStatus.DUE_SOON, ServiceStatus.of(vehicle(14_500.0, 10_000.0)));
        assertEquals(ServiceStatus.DUE, ServiceStatus.of(vehicle(15_000.0, 10_000.0)));
        assertEquals(-200.0, ServiceStatus.kmToService(vehicle(15_200.0, 10_000.0)));
    }

    @Test
    void recordingAServiceRestartsTheIntervalAtTheCurrentReading() {
        VehicleRepository repository = mock(VehicleRepository.class);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        Vehicle car = vehicle(15_200.0, 10_000.0);
        when(repository.findById(7L)).thenReturn(Optional.of(car));
        VehicleServiceImp vehicles = new VehicleServiceImp(repository, mock(FuelCardRepository.class), mock(FuelTypeRepository.class));

        assertThrows(IllegalArgumentException.class, () -> vehicles.recordService(7L, new VehicleServiceRequest(15_000.0, null)));

        vehicles.recordService(7L, new VehicleServiceRequest(null, null));

        assertEquals(15_200.0, car.getLastServiceOdometerKm());
        assertEquals(LocalDate.now(), car.getLastServiceDate());
        assertEquals(ServiceStatus.OK, ServiceStatus.of(car));
    }

    @Test
    void leadersRankDriversByFuelUsedAndVehiclesByTrips() {
        List<TripReport.Row> rows = List.of(
                row("Jane", "AAA 1", 100.0, null, 20.0),
                row("Jane", "BBB 2", 50.0, 60.0, 15.0),
                row("Tom", "BBB 2", 80.0, null, 40.0),
                row("Tom", "BBB 2", 10.0, null, null));

        List<TripReport.Leader> fuel = ReportService.leaders(rows, TripReport.Row::driver, TripReport.Row::department,
                Comparator.comparingDouble(TripReport.Leader::fuelUsedLitres));
        assertEquals("Tom", fuel.get(0).name());
        assertEquals(40.0, fuel.get(0).fuelUsedLitres());
        assertEquals(35.0, fuel.get(1).fuelUsedLitres());

        List<TripReport.Leader> vehicles = ReportService.leaders(rows, TripReport.Row::vehicle, TripReport.Row::vehicleModel,
                Comparator.comparingLong(TripReport.Leader::trips));
        assertEquals("BBB 2", vehicles.get(0).name());
        assertEquals(3, vehicles.get(0).trips());
        // actual km when recorded, else planned
        assertEquals(150.0, vehicles.get(0).distanceKm());
    }

    @Test
    void usageRanksDepartmentsByFuelAndPeopleByDistanceInFull() {
        List<TripReport.Row> rows = List.of(
                row("Jane", "Ops", "AAA 1", 100.0, null, 20.0),
                row("Jane", "Ops", "BBB 2", 50.0, 300.0, 15.0),
                row("Tom", "Finance", "BBB 2", 80.0, null, 40.0),
                row("Ann", "Finance", "CCC 3", 10.0, null, null),
                row("Bob", "HR", "DDD 4", 5.0, null, 1.0),
                row("Sue", "IT", "EEE 5", 1.0, null, null),
                row("Max", "Stores", "FFF 6", 2.0, null, null));

        List<TripReport.Leader> departments = UsageReport.By.DEPARTMENT.rank(rows);
        assertEquals(List.of("Finance", "Ops", "HR"), departments.stream().map(TripReport.Leader::name).toList().subList(0, 3));
        assertEquals(40.0, departments.get(0).fuelUsedLitres());
        assertEquals(5, departments.size());

        List<TripReport.Leader> people = UsageReport.By.DRIVER.rank(rows);
        assertEquals(6, people.size()); // every person, not just the top 5
        assertEquals("Jane", people.get(0).name());
        assertEquals(400.0, people.get(0).distanceKm()); // actual km when recorded, else planned
        assertEquals("Ops", people.get(0).detail());

        assertEquals("BBB 2", UsageReport.By.VEHICLE.rank(rows).get(0).name());
    }

    private static Vehicle vehicle(Double odometerKm, Double lastServiceKm) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(7L);
        vehicle.setRegistrationNumber("AZW 6789");
        vehicle.setOdometerKm(odometerKm);
        vehicle.setLastServiceOdometerKm(lastServiceKm);
        return vehicle;
    }

    private static TripReport.Row row(String driver, String vehicle, Double plannedKm, Double actualKm, Double fuelUsed) {
        return row(driver, "Ops", vehicle, plannedKm, actualKm, fuelUsed);
    }

    private static TripReport.Row row(String driver, String department, String vehicle, Double plannedKm, Double actualKm,
                                      Double fuelUsed) {
        return new TripReport.Row(1L, null, null, driver, department, vehicle, "Toyota Hilux", "A → B", "Local",
                plannedKm, actualKm, null, null, null, fuelUsed, null, null, null, null, null, "Completed", null, null);
    }

    @Test
    void daysLateCountsFromTheReturnDateToTheReturnOrToday() {
        LocalDate due = LocalDate.of(2026, 10, 10);
        TransportRequest trip = new TransportRequest();
        trip.setRequiredTo(due);
        VehicleAllocation allocation = new VehicleAllocation();

        trip.setStatus(RequestStatus.COMPLETED);
        allocation.setReturnDate(due.atTime(17, 0));
        assertNull(ReportService.daysLate(trip, allocation, due.plusDays(9)));
        allocation.setReturnDate(due.plusDays(2).atTime(8, 0));
        assertEquals(2L, ReportService.daysLate(trip, allocation, due.plusDays(9)));

        // still out: late so far
        trip.setStatus(RequestStatus.FUEL_APPROVED);
        allocation.setReturnDate(null);
        assertEquals(4L, ReportService.daysLate(trip, allocation, due.plusDays(4)));
        assertNull(ReportService.daysLate(trip, allocation, due));
    }
}
