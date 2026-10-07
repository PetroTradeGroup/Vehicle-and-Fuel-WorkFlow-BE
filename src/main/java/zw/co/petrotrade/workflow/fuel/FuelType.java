package zw.co.petrotrade.workflow.fuel;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "fuel_types")
public class FuelType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;
}
