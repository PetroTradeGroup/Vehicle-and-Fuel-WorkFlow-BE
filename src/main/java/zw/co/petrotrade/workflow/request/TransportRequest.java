package zw.co.petrotrade.workflow.request;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "transport_requests")
public class TransportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String driverName;

    private String department;

    private String jobTitle;
    private String licenceNumber;
    private LocalDate requiredFrom;

    private LocalDate requiredTo;

    private String purpose;

    private String destination;

    private Double distanceKm;
    private Double fuelRequiredLitres;
    private Double tollFees;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}