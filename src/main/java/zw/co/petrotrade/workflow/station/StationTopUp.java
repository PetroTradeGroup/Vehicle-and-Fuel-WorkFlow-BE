package zw.co.petrotrade.workflow.station;

import jakarta.persistence.*;
import lombok.Data;

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

    private String reason;

    private String requestedBy;

    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    private TopUpStatus status;
}
