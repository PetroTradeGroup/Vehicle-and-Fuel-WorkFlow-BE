package zw.co.petrotrade.workflow.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import zw.co.petrotrade.workflow.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.Map;

// The signed-in person's notifications, and the system admin's test email
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;
    private final NotificationRepository inbox;
    private final CurrentUser currentUser;

    public record NotificationResponse(Long id, String title, String message, String link, LocalDateTime createdAt, boolean read) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getTitle(), n.getMessage(), n.getLink(), n.getCreatedAt(), n.getReadAt() != null);
        }
    }

    public record TestEmail(@NotBlank @Email String to) {}

    // the latest 50, newest first, with how many are unread in all
    @GetMapping
    public Map<String, Object> mine() {
        Long me = currentUser.get().getId();
        return Map.of(
                "unread", inbox.countByUserIdAndReadAtIsNull(me),
                "items", inbox.findTop50ByUserIdOrderByIdDesc(me).stream().map(NotificationResponse::from).toList());
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable Long id) {
        Notification n = inbox.findByIdAndUserId(id, currentUser.get().getId()).orElseThrow();
        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now());
            inbox.save(n);
        }
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll() {
        inbox.markAllRead(currentUser.get().getId(), LocalDateTime.now());
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public Map<String, String> test(@Valid @RequestBody TestEmail body) {
        return Map.of("message", notifications.sendTest(body.to().trim(), currentUser.get().getFullName()));
    }

}
