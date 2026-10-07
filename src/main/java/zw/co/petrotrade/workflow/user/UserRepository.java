package zw.co.petrotrade.workflow.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByKeycloakId(String keycloakId);

    // people with a role; no role means they signed in but are still awaiting access
    List<User> findByRoleIsNotNull();

    List<User> findByRoleIsNullOrderByIdDesc();

    List<User> findByRoleAndDepartment_Id(Role role, Long departmentId);

}
