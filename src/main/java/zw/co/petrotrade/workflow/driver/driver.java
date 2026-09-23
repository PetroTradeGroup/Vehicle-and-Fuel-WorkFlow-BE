package zw.co.petrotrade.workflow.driver;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "drivers")
public class driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeNumber;

    private String fullName;

    private String licenceNumber;

    private String department;

    private String jobTitle;
}