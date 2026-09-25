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
}
