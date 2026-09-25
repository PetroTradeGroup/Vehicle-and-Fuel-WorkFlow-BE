package zw.co.petrotrade.workflow.fuel;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "fuel_cards",
        uniqueConstraints = @UniqueConstraint(columnNames = {"holder_type", "holder_id"}))
public class FuelCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "holder_type", nullable = false)
    private CardHolderType holderType;

    @Column(name = "holder_id", nullable = false)
    private Long holderId;

    private Double balanceLitres;

    private Boolean active;

    @Version
    private Long version;
}
