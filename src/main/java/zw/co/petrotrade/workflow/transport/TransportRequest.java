package zw.co.petrotrade.workflow.transport;

import jakarta.persistence.*;
import lombok.Data;
import zw.co.petrotrade.workflow.fuel.FuelType;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "transport_requests")
public class TransportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long driverId;
    private String driverName;
    private String department;
    private String jobTitle;
    private String licenceNumber;
    private LocalDate requiredFrom;
    private LocalDate requiredTo;
    private String purpose;
    private String destination;
    private String startingPoint;
    private Double distanceKm;
    private Double fuelRequiredLitres;
    @Enumerated(EnumType.STRING)
    private FuelType fuelType;
    private String requestedVehicleReg;
    private Double tollFees;
    private String createdBy;
    private String signature;
    private String notes;
    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}