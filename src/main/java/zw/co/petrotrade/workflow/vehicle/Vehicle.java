package zw.co.petrotrade.workflow.vehicle;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import zw.co.petrotrade.workflow.fuel.FuelType;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String registrationNumber;

    private String make;

    private String model;

    // what it runs on; copied onto a request when the vehicle is allocated to it
    @ManyToOne
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    private VehicleStatus status;

    // last known odometer reading (km): set when the vehicle is added, then rolled forward on each return
    private Double odometerKm;

    // odometer and date at the last service; the next one is due ServiceStatus.INTERVAL_KM later
    private Double lastServiceOdometerKm;

    private LocalDate lastServiceDate;

    // two concurrent allocations of the same vehicle: the second save fails instead of double-booking
    @Version
    private Long version;
}