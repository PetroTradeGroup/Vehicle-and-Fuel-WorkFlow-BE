package zw.co.petrotrade.workflow.user;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String employeeNumber;

    private String fullName;

    private String email;

    private String department;

    private String licenceNumber;

    private String jobTitle;

    @Enumerated(EnumType.STRING)
    private Role role;
}