package zw.co.petrotrade.workflow.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Office 365 mail through Microsoft Graph, as an Entra app with the Mail.Send application permission:
// no mailbox password, and unaffected by Microsoft switching off password sign-in for SMTP.
@Component
public class GraphMailSender {

    private final RestClient http = RestClient.create();
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String sender;

    private String token;
    private Instant tokenExpires = Instant.EPOCH;

    public GraphMailSender(
            @Value("${workflow.mail.graph.tenant-id:}") String tenantId,
            @Value("${workflow.mail.graph.client-id:}") String clientId,
            @Value("${workflow.mail.graph.client-secret:}") String clientSecret,
            @Value("${workflow.mail.from:}") String sender) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.sender = sender;
    }

    public boolean configured() {
        return !tenantId.isBlank() && !clientId.isBlank() && !clientSecret.isBlank() && !sender.isBlank();
    }

    // sent from the sender mailbox; Graph answers 202 once it has accepted the message
    public void send(String[] to, String[] cc, String subject, String text) {
        Map<String, Object> message = Map.of(
                "subject", subject,
                "body", Map.of("contentType", "Text", "content", text),
                "toRecipients", recipients(to),
                "ccRecipients", recipients(cc));
        http.post().uri("https://graph.microsoft.com/v1.0/users/{sender}/sendMail", sender)
                .header("Authorization", "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", message, "saveToSentItems", false))
                .retrieve().toBodilessEntity();
    }

    private static List<Map<String, Object>> recipients(String[] addresses) {
        return addresses == null ? List.of() : Arrays.stream(addresses)
                .filter(a -> a != null && !a.isBlank())
                .map(a -> Map.<String, Object>of("emailAddress", Map.of("address", a)))
                .toList();
    }

    // tokens last about an hour; reuse one until a minute before it runs out
    private synchronized String token() {
        if (token == null || Instant.now().isAfter(tokenExpires)) {
            var form = new LinkedMultiValueMap<String, String>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("scope", "https://graph.microsoft.com/.default");
            Map<?, ?> response = http.post()
                    .uri("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token", tenantId)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(Map.class);
            token = (String) response.get("access_token");
            tokenExpires = Instant.now().plusSeconds(((Number) response.get("expires_in")).longValue() - 60);
        }
        return token;
    }
}
