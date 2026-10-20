package zw.co.petrotrade.workflow.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import zw.co.petrotrade.workflow.transport.TransportRequest;
import zw.co.petrotrade.workflow.transport.TripType;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

// Every workflow event goes through here: each person gets an in-app notification, and an email too
// when they have an address and mail is set up. A mail outage never fails the workflow step.
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;
    private final MailOutbox mail;
    private final UserRepository userRepository;

    @Value("${workflow.notifications.hr-manager-email:}")
    private String hrManagerEmail;

    @Value("${workflow.notifications.it-department-email:}")
    private String itDepartmentEmail;

    // the app's address, so emails can link straight to the page; blank leaves the link out
    @Value("${workflow.notifications.app-url:}")
    private String appUrl;

    // HR & Admin's final approval; the configured HR and IT addresses are copied in as before
    public void notifyFuelRequestSubmitted(TransportRequest request) {
        notify(userRepository.findByRole(Role.HR_ADMIN_MANAGER),
                "Trip & fuel approval needed - Request #" + request.getId(),
                "A trip and its fuel require HR & Admin approval.\n\n"
                        + "Driver/Officer: " + request.getDriverName() + "\n"
                        + "Department: " + request.getDepartment() + "\n"
                        + "Destination: " + request.getDestination() + "\n"
                        + "Trip type: " + TripType.of(request.getDistanceKm()) + " (" + request.getDistanceKm() + " km)\n"
                        + "Fuel required: " + request.getFuelRequiredLitres() + " litres\n",
                requestLink(request.getId()),
                List.of(hrManagerEmail, itDepartmentEmail));
    }

    public void notifyVehicleAdmins(String title, String text, String link) {
        notifyRoles(List.of(Role.VEHICLE_ADMIN), title, text, link);
    }

    public void notifyRoles(Collection<Role> roles, String title, String text, String link) {
        notify(userRepository.findByRoleInOrderById(roles), title, text, link, List.of());
    }

    public void notifyUser(Long userId, String title, String text, String link) {
        if (userId == null) return;
        notify(userRepository.findById(userId).stream().toList(), title, text, link, List.of());
    }

    // the head of the department: its HOD, or the retail manager who heads it
    public void notifyDepartmentHeads(Long departmentId, String title, String text, String link) {
        if (departmentId == null) return;
        notify(userRepository.findByRoleInAndDepartment_Id(Role.DEPARTMENT_HEADS, departmentId), title, text, link, List.of());
    }

    public void notifyStationAdmins(Long stationId, String title, String text, String link) {
        if (stationId == null) return;
        notify(userRepository.findByRoleAndStationId(Role.STATION_ADMIN, stationId), title, text, link, List.of());
    }

    public static String requestLink(Long requestId) {
        return "/requests/" + requestId;
    }

    // one notification per person; one email to everyone who has an address, plus any extra addresses
    private void notify(List<User> people, String title, String text, String link, List<String> extraEmails) {
        Collection<User> recipients = people.stream().filter(u -> u.getId() != null)
                .collect(LinkedHashMap<Long, User>::new, (m, u) -> m.putIfAbsent(u.getId(), u), LinkedHashMap::putAll)
                .values();
        if (recipients.isEmpty()) {
            log.info("Nobody to notify: {}", title);
        }
        LocalDateTime now = LocalDateTime.now();
        notifications.saveAll(recipients.stream().map(u -> {
            Notification n = new Notification();
            n.setUserId(u.getId());
            n.setTitle(title);
            n.setMessage(text);
            n.setLink(link);
            n.setCreatedAt(now);
            return n;
        }).toList());

        String[] to = Stream.concat(recipients.stream().map(User::getEmail), extraEmails.stream())
                .filter(Objects::nonNull).map(String::trim).filter(e -> !e.isBlank()).distinct().toArray(String[]::new);
        if (to.length == 0) return;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(title);
        message.setText(text.strip() + "\n\n" + (appUrl.isBlank() || link == null
                ? "Open the vehicle & fuel workflow to see it."
                : "Open it: " + appUrl.replaceAll("/+$", "") + link) + "\n");
        mail.sendLater(message);
    }

    // a system admin checking the mail settings: unlike the notifications, failures come back to them
    public String sendTest(String to, String requestedBy) {
        if (mail.from().isBlank()) {
            throw new IllegalStateException("No sender mailbox is set. Set MAIL_FROM (or MAIL_USERNAME for SMTP) and restart.");
        }
        String via = mail.via();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Test email from the vehicle & fuel workflow");
        message.setText("This is a test sent by " + requestedBy + " through " + via + " from " + mail.from() + ".\n"
                + "If you can read it, workflow notifications will reach people.\n");
        try {
            mail.deliver(message);
        } catch (MailException | RestClientException e) {
            log.error("Test email to {} failed", to, e);
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            throw new IllegalStateException("Sending through " + via + " failed: " + root.getMessage());
        }
        return "Sent to " + to + " through " + via + " from " + mail.from() + ".";
    }
}
