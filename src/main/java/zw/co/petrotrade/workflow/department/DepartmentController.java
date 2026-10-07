package zw.co.petrotrade.workflow.department;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.department.dto.DepartmentRequest;
import zw.co.petrotrade.workflow.department.dto.DepartmentResponse;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.user.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository repository;
    private final UserRepository users;

    // everyone needs the names (pickers, labels); switched-off ones are included and marked
    @GetMapping
    public List<DepartmentResponse> list() {
        List<User> people = users.findByRoleIsNotNull();
        return repository.findAllByOrderByNameAsc().stream().map(d -> {
            List<User> members = people.stream()
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(d.getId()))
                    .toList();
            User head = members.stream().filter(u -> u.getRole() == Role.HOD).findFirst().orElse(null);
            return new DepartmentResponse(d.getId(), d.getName(), d.isActive(),
                    head == null ? null : head.getId(), head == null ? null : head.getFullName(), members.size());
        }).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
        Department department = new Department();
        return save(department, request);
    }

    // rename or switch on/off; departments are never deleted because requests refer to them
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return save(repository.findById(id).orElseThrow(), request);
    }

    private DepartmentResponse save(Department department, DepartmentRequest request) {
        String name = request.name().trim();
        repository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(department.getId()))
                .ifPresent(other -> {
                    throw new IllegalStateException("There's already a department called " + other.getName() + ".");
                });
        department.setName(name);
        if (request.active() != null) {
            department.setActive(request.active());
        }
        Long id = repository.save(department).getId();
        return list().stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow();
    }
}
