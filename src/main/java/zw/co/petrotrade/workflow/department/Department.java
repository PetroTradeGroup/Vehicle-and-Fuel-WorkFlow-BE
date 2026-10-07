package zw.co.petrotrade.workflow.department;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // switched off: can't be picked for people any more, but old requests keep it
    @Column(nullable = false)
    private boolean active = true;
}
