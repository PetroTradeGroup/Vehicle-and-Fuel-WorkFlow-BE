package zw.co.petrotrade.workflow.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public User create(User user) {
        if (repository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalStateException("Username " + user.getUsername() + " is already taken");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return repository.save(user);
    }
}
