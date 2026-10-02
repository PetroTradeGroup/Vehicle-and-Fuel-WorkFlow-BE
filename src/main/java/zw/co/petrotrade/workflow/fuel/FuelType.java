package zw.co.petrotrade.workflow.fuel;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "fueltype")
public class FuelType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    String name;
    String code;

}
