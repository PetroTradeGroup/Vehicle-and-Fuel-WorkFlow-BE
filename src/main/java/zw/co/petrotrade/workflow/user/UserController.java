package zw.co.petrotrade.workflow.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.user.dto.UserCreateRequest;
import zw.co.petrotrade.workflow.user.dto.UserResponse;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final UserRepository repository;

    @PostMapping
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return UserResponse.from(service.create(request.toEntity()));
    }

    @GetMapping
    public List<UserResponse> list() {
        return repository.findAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(repository.findById(id).orElseThrow());
    }
}
