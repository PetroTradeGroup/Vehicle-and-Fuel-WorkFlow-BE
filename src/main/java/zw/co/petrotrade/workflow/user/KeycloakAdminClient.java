package zw.co.petrotrade.workflow.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KeycloakAdminClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};

    private final RestClient http = RestClient.create();
    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String adminClientId;
    private final String adminClientSecret;

    public KeycloakAdminClient(
            @Value("${workflow.keycloak.server-url}") String serverUrl,
            @Value("${workflow.keycloak.realm}") String realm,
            @Value("${workflow.keycloak.client-id}") String clientId,
            @Value("${workflow.keycloak.admin-client-id}") String adminClientId,
            @Value("${workflow.keycloak.admin-client-secret}") String adminClientSecret) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
    }

    public Optional<String> findUserId(String username) {
        List<Map<String, Object>> users = admin().get()
                .uri(adminUrl("/users?exact=true&username={username}"), username)
                .retrieve().body(LIST);
        return users == null || users.isEmpty() ? Optional.empty() : Optional.of((String) users.get(0).get("id"));
    }

    public String createUser(User user, String temporaryPassword) {
        String[] names = user.getFullName().trim().split("\\s+", 2);
        Map<String, Object> body = new HashMap<>(Map.of(
                "username", user.getUsername(),
                "firstName", names[0],
                "lastName", names.length > 1 ? names[1] : names[0],
                "enabled", true,
                "credentials", List.of(Map.of("type", "password", "value", temporaryPassword, "temporary", true))));
        // left out when unknown: Keycloak then asks for it at first sign-in instead of storing a blank
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            body.put("email", user.getEmail());
        }
        try {
            var response = admin().post().uri(adminUrl("/users"))
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().toBodilessEntity();
            String location = response.getHeaders().getLocation().getPath();
            return location.substring(location.lastIndexOf('/') + 1);
        } catch (HttpClientErrorException.Conflict e) {
            throw new IllegalStateException("A Keycloak account with that username or email already exists");
        }
    }


    public void setRole(String userId, Role role) {
        RestClient admin = admin();
        String clientUuid = clientUuid(admin);
        String mappings = adminUrl("/users/" + userId + "/role-mappings/clients/" + clientUuid);
        List<Map<String, Object>> current = admin.get().uri(mappings).retrieve().body(LIST);
        if (current != null && !current.isEmpty()) {
            admin.method(HttpMethod.DELETE).uri(mappings)
                    .contentType(MediaType.APPLICATION_JSON).body(current)
                    .retrieve().toBodilessEntity();
        }
        Map<?, ?> roleRep = admin.get().uri(adminUrl("/clients/" + clientUuid + "/roles/" + role.name()))
                .retrieve().body(Map.class);
        admin.post().uri(mappings).contentType(MediaType.APPLICATION_JSON).body(List.of(roleRep))
                .retrieve().toBodilessEntity();
    }

    public void deleteUser(String userId) {
        admin().delete().uri(adminUrl("/users/" + userId)).retrieve().toBodilessEntity();
    }

    private String clientUuid(RestClient admin) {
        List<Map<String, Object>> clients = admin.get().uri(adminUrl("/clients?clientId={clientId}"), clientId)
                .retrieve().body(LIST);
        if (clients == null || clients.isEmpty()) {
            throw new IllegalStateException("Keycloak client " + clientId + " not found; run scripts/keycloak-setup.sh");
        }
        return (String) clients.get(0).get("id");
    }

    private String adminUrl(String path) {
        return serverUrl + "/admin/realms/" + realm + path;
    }

    private RestClient admin() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);
        Map<?, ?> token = http.post()
                .uri(serverUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(Map.class);
        return http.mutate().defaultHeader("Authorization", "Bearer " + token.get("access_token")).build();
    }
}
