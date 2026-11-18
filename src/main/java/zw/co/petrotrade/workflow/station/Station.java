package zw.co.petrotrade.workflow.station;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "stations")
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String location;

    // the generator's running hours at the last top-up request; like an odometer it only goes up
    private Double generatorHours;
}
