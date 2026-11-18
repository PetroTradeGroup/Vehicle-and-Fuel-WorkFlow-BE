package zw.co.petrotrade.workflow.station;

import jakarta.persistence.*;
import lombok.Data;
import zw.co.petrotrade.workflow.fuel.FuelType;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "station_top_ups")
public class StationTopUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long stationId;

    private Double litres;

    // Diesel, Petrol, ...; empty on top-ups from before it was asked
    @ManyToOne
    private FuelType fuelType;

    // empty on top-ups from before it was asked (those were for the generator)
    @Enumerated(EnumType.STRING)
    private TopUpPurpose purpose;

    // generator running hours when the fuel was requested; only for generator top-ups
    private Double generatorHours;

    private String reason;

    private String requestedBy;

    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    private TopUpStatus status;
}
