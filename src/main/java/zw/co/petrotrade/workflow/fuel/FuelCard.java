package zw.co.petrotrade.workflow.fuel;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "fuel_cards")
public class FuelCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardNumber;

    private Double balanceLitres;

    private Boolean active;
}
