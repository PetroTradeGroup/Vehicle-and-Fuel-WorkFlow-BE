package zw.co.petrotrade.workflow.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.user.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// The signed-in person: their app profile, matched to the token's Keycloak account
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository users;

    @Value("${workflow.keycloak.client-id}")
    private String clientId;

    public List<Role> roles() {
        return SecurityConfig.clientRoles(jwt(), clientId).stream()
                .filter(name -> Arrays.stream(Role.values()).anyMatch(r -> r.name().equals(name)))
                .map(Role::valueOf)
                .toList();
    }

    public boolean has(Role... any) {
        List<Role> mine = roles();
        return Arrays.stream(any).anyMatch(mine::contains);
    }

    public void require(Role... any) {
        if (!has(any)) {
            // "Only a vehicle admin or system admin can do this."
            String who = Arrays.stream(any).map(role -> switch (role) {
                case STATION_ADMIN -> "station admin";
                case DRIVER -> "driver";
                case HOD -> "head of department";
                case HR_ADMIN_MANAGER -> "HR & Admin manager";
                case VEHICLE_ADMIN -> "vehicle admin";
                case SYSTEM_ADMIN -> "system admin";
            }).collect(Collectors.joining(" or "));
            throw new AccessDeniedException("Only a " + who + " can do this.");
        }
    }

    // A station admin without an oversight role is limited to their own station.
    // Returns that station's id, or null for people who may see every station.
    public Long stationScope() {
        if (!has(Role.STATION_ADMIN) || has(Role.HR_ADMIN_MANAGER, Role.VEHICLE_ADMIN, Role.SYSTEM_ADMIN)) {
            return null;
        }
        Long stationId = get().getStationId();
        if (stationId == null) {
            throw new AccessDeniedException("Your account isn't linked to a station yet. Ask a system admin to set your station.");
        }
        return stationId;
    }

    // Station pages and reports: oversight roles see any station, a station admin only their own
    public void requireStation(Long stationId) {
        Long own = stationScope();
        if (own != null) {
            if (!own.equals(stationId)) {
                throw new AccessDeniedException("You can only see your own station.");
            }
        } else if (!has(Role.HR_ADMIN_MANAGER, Role.VEHICLE_ADMIN, Role.SYSTEM_ADMIN)) {
            throw new AccessDeniedException("You don't have permission to see station reports.");
        }
    }

    // Finds the profile by Keycloak id; links a profile created before sign-in existed by username;
    // creates one for an account that was given a vehicle role straight in Keycloak.
    // Someone signing in without a vehicle role still gets a profile, with no role: that puts them on the
    // admins' "awaiting access" list. Deliberately not @Transactional, so that save survives the denial below.
    public User get() {
        List<Role> roles = roles();
        Jwt jwt = jwt();
        User user = users.findByKeycloakId(jwt.getSubject())
                .or(() -> users.findByUsername(jwt.getClaimAsString("preferred_username"))
                        .filter(u -> u.getKeycloakId() == null))
                .orElseGet(() -> {
                    User created = new User();
                    created.setUsername(jwt.getClaimAsString("preferred_username"));
                    created.setFullName(jwt.getClaimAsString("name") != null ? jwt.getClaimAsString("name") : created.getUsername());
                    created.setEmail(jwt.getClaimAsString("email"));
                    return created;
                });
        Role highest = roles.stream().max(Enum::compareTo).orElse(null);
        if (user.getId() == null || user.getKeycloakId() == null || user.getRole() != highest) {
            user.setKeycloakId(jwt.getSubject());
            user.setRole(highest);
            user = users.save(user);
        }
        if (highest == null) {
            throw new AccessDeniedException("Your request for access has been sent to the administrators. "
                    + "You can use the vehicle & fuel workflow once one of them gives you a role.");
        }
        return user;
    }

    private Jwt jwt() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new AccessDeniedException("Not signed in");
    }
}
