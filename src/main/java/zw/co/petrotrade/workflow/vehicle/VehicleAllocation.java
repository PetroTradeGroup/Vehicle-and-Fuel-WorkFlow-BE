package zw.co.petrotrade.workflow.vehicle;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vehicle_allocations")
public class VehicleAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requestId;

    private Long vehicleId;

    private String allocatedBy;

    private LocalDateTime allocationDate;

    private String returnedBy;

    private LocalDateTime returnDate;

    // recorded on return: what the trip actually took
    private Double fuelUsedLitres;

    private Double distanceTravelledKm;
}