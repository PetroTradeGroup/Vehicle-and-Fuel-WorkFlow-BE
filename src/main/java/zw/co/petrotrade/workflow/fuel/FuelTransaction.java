package zw.co.petrotrade.workflow.fuel;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "fuel_transactions")
public class FuelTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // set for trip fuel; stationTopUpId is set instead for generator top-ups
    private Long requestId;

    private Long stationTopUpId;

    private Long fuelCardId;

    private Long vehicleId;

    private Long driverId;

    private Double allocatedLitres;

    private Double consumedLitres;

    private Double remainingBalance;

    private LocalDateTime transactionDate;

    // trip fuel waits at false until IT loads it; null on rows from before IT loaded fuel, which count as loaded
    private Boolean loaded;

    private String loadedBy;

    private LocalDateTime loadedAt;
}
