package zw.co.petrotrade.workflow.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.security.CurrentUser;
import zw.co.petrotrade.workflow.user.dto.CurrentUserResponse;
import zw.co.petrotrade.workflow.user.dto.UserCreateRequest;
import zw.co.petrotrade.workflow.user.dto.UserDirectoryEntry;
import zw.co.petrotrade.workflow.user.dto.UserResponse;
import zw.co.petrotrade.workflow.user.dto.UserUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final UserRepository repository;
    private final CurrentUser currentUser;

    @GetMapping("/me")
    public CurrentUserResponse me() {
        return new CurrentUserResponse(UserResponse.from(currentUser.get()), currentUser.roles());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return UserResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return UserResponse.from(service.update(id, request));
    }

    // full profiles (email, employee no., licence): system admins only
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<UserResponse> list() {
        return repository.findByRoleIsNotNull().stream().map(UserResponse::from).toList();
    }

    // just names, for pickers and labels (driver choice, report filters, card holders)
    @GetMapping("/directory")
    public List<UserDirectoryEntry> directory() {
        return repository.findByRoleIsNotNull().stream()
                .map(u -> new UserDirectoryEntry(u.getId(), u.getFullName(), u.getRole()))
                .toList();
    }

    // signed in without a vehicle role; granting access is a PUT /{id} with a role
    @GetMapping("/pending")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<UserResponse> pending() {
        return repository.findByRoleIsNullOrderByIdDesc().stream().map(UserResponse::from).toList();
    }

    // dismisses an access request; they reappear if they sign in again
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void dismiss(@PathVariable Long id) {
        User user = repository.findById(id).orElseThrow();
        if (user.getRole() != null) {
            throw new IllegalStateException("Only people still awaiting access can be dismissed");
        }
        repository.delete(user);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(repository.findById(id).orElseThrow());
    }
}
