package zw.co.petrotrade.workflow.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.department.Department;
import zw.co.petrotrade.workflow.department.DepartmentRepository;
import zw.co.petrotrade.workflow.station.StationRepository;
import zw.co.petrotrade.workflow.user.dto.UserCreateRequest;
import zw.co.petrotrade.workflow.user.dto.UserUpdateRequest;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final KeycloakAdminClient keycloak;
    private final StationRepository stationRepository;
    private final DepartmentRepository departmentRepository;

    // Reuses an existing Keycloak login (e.g. a help desk account) or creates one, then gives it the role
    public User create(UserCreateRequest request) {
        User existing = repository.findByUsername(request.username()).orElse(null);
        if (existing != null && existing.getRole() != null) {
            throw new IllegalStateException("Username " + request.username() + " is already taken");
        }
        if (existing != null) {
            // they're on the awaiting-access list: adding them is granting access
            return update(existing.getId(), new UserUpdateRequest(request.employeeNumber(), request.fullName(),
                    request.email(), request.departmentId(), request.licenceNumber(), request.jobTitle(), request.role(),
                    request.stationId()));
        }
        checkStation(request.role(), request.stationId());
        User user = request.toEntity();
        user.setDepartment(checkDepartment(request.role(), request.departmentId(), null));

        String keycloakId = keycloak.findUserId(request.username()).orElse(null);
        boolean newLogin = keycloakId == null;
        if (newLogin) {
            if (request.password() == null || request.password().isBlank()) {
                throw new IllegalArgumentException("There's no help desk login called \"" + request.username()
                        + "\" yet, so one will be created. Enter a temporary password for them; they'll choose their own at first sign-in.");
            }
            keycloakId = keycloak.createUser(user, request.password());
        } else if (repository.findByKeycloakId(keycloakId).isPresent()) {
            throw new IllegalStateException("That Keycloak account already has a profile here");
        }

        try {
            keycloak.setRole(keycloakId, request.role());
            user.setKeycloakId(keycloakId);
            return repository.save(user);
        } catch (RuntimeException e) {
            // don't leave a login behind that the app knows nothing about
            if (newLogin) {
                keycloak.deleteUser(keycloakId);
            }
            throw e;
        }
    }

    public User update(Long id, UserUpdateRequest request) {
        User user = repository.findById(id).orElseThrow();
        checkStation(request.role(), request.stationId());
        Department department = checkDepartment(request.role(), request.departmentId(), user);
        if (request.role() != user.getRole() && user.getKeycloakId() != null) {
            keycloak.setRole(user.getKeycloakId(), request.role());
        }
        user.setFullName(request.fullName());
        user.setEmployeeNumber(request.employeeNumber());
        user.setEmail(request.email());
        user.setDepartment(department);
        user.setLicenceNumber(request.licenceNumber());
        user.setJobTitle(request.jobTitle());
        user.setRole(request.role());
        user.setStationId(request.role() == Role.STATION_ADMIN ? request.stationId() : null);
        return repository.save(user);
    }

    // A department must exist and be switched on (unless they're already in it); a head of department needs
    // one, and each department has only one head
    private Department checkDepartment(Role role, Long departmentId, User user) {
        if (departmentId == null) {
            if (role == Role.HOD) {
                throw new IllegalArgumentException("Choose the department this head of department runs.");
            }
            return null;
        }
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("That department doesn't exist."));
        boolean alreadyIn = user != null && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId);
        if (!department.isActive() && !alreadyIn) {
            throw new IllegalArgumentException(department.getName() + " is switched off. Pick another department, or switch it back on on the Departments page.");
        }
        if (role == Role.HOD) {
            repository.findByRoleAndDepartment_Id(Role.HOD, departmentId).stream()
                    .filter(head -> user == null || !head.getId().equals(user.getId()))
                    .findFirst()
                    .ifPresent(head -> {
                        throw new IllegalStateException(head.getFullName() + " is already head of " + department.getName()
                                + ". A department has one head: change their role first, then try again.");
                    });
        }
        return department;
    }

    // a station admin must run a real station
    private void checkStation(Role role, Long stationId) {
        if (role != Role.STATION_ADMIN) return;
        if (stationId == null) {
            throw new IllegalArgumentException("Choose the station this station admin runs.");
        }
        if (!stationRepository.existsById(stationId)) {
            throw new IllegalArgumentException("That station doesn't exist.");
        }
    }
}
