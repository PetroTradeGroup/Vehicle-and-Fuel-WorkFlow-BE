package zw.co.petrotrade.workflow.user;

import jakarta.persistence.*;
import lombok.Data;
import zw.co.petrotrade.workflow.department.Department;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // the Keycloak account ("sub" in tokens); Keycloak holds the password and the roles
    @Column(unique = true)
    private String keycloakId;

    private String username;

    private String employeeNumber;

    private String fullName;

    private String email;

    @ManyToOne
    private Department department;

    private String licenceNumber;

    private String jobTitle;

    // highest vehicle role in Keycloak, kept for display; access checks use the token's roles
    @Enumerated(EnumType.STRING)
    private Role role;

    // for a station admin: the one station they run
    private Long stationId;
}