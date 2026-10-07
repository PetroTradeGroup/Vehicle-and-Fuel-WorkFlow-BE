package zw.co.petrotrade.workflow.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import zw.co.petrotrade.workflow.user.Role;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// Keycloak (shared "helpdesk" realm) logs people in; this API only checks the bearer token.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${workflow.keycloak.client-id}")
    private String clientId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] anyAppRole = Arrays.stream(Role.values()).map(Role::name).toArray(String[]::new);
        // everyone except station admins, who only get the station endpoints
        String[] staffRoles = Arrays.stream(Role.values()).filter(r -> r != Role.STATION_ADMIN).map(Role::name).toArray(String[]::new);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health").permitAll()
                        // reachable without a vehicle role so the frontend can explain "no access"
                        .requestMatchers("/api/users/me").authenticated()
                        // station admins: their station's page, report and top-ups (scoped further in the controllers)
                        .requestMatchers("/api/stations/**", "/api/station-top-ups/**").hasAnyRole(anyAppRole)
                        // a help desk account with no vehicle role gets nothing else
                        .requestMatchers("/api/**").hasAnyRole(staffRoles)
                        .anyRequest().authenticated())
                // rejections made here never reach GlobalExceptionHandler, so give them the same JSON shape
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, ex) ->
                        writeError(response, HttpStatus.FORBIDDEN,
                                "Your account doesn't have access to this. Ask an administrator for a role.")))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Your session has ended. Please sign in again.")));
        return http.build();
    }

    // same shape as error.ErrorResponse; messages are fixed text, so no JSON escaping is needed
    private static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message + "\"}");
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> clientRoles(jwt, clientId).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    // Keycloak puts client roles under resource_access.<client-id>.roles; realm roles (the help desk's) are ignored
    static List<String> clientRoles(Jwt jwt, String clientId) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || !(resourceAccess.get(clientId) instanceof Map<?, ?> client)
                || !(client.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().map(String::valueOf).toList();
    }
}
