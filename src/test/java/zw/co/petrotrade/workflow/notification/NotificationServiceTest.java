package zw.co.petrotrade.workflow.notification;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import zw.co.petrotrade.workflow.user.Role;
import zw.co.petrotrade.workflow.user.User;
import zw.co.petrotrade.workflow.user.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// everyone notified gets it in the app; those with an address also by email, in one message
class NotificationServiceTest {

    private final NotificationRepository inbox = mock(NotificationRepository.class);
    private final MailOutbox mail = mock(MailOutbox.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationService notifications = new NotificationService(inbox, mail, users);

    @Test
    @SuppressWarnings("unchecked")
    void eachPersonGetsOneInAppNotificationAndThoseWithAnAddressAnEmail() {
        ReflectionTestUtils.setField(notifications, "appUrl", "https://fleet.example/");
        User withEmail = user(1L, "va@petrotrade.co.zw");
        // listed twice (e.g. two roles) and one with no address
        when(users.findByRoleInOrderById(List.of(Role.VEHICLE_ADMIN))).thenReturn(List.of(withEmail, withEmail, user(2L, null)));

        notifications.notifyVehicleAdmins("Vehicle needed - Request #5", "Allocate a vehicle.", "/requests/5");

        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(inbox).saveAll(saved.capture());
        assertEquals(List.of(1L, 2L), saved.getValue().stream().map(Notification::getUserId).toList());
        assertEquals("/requests/5", saved.getValue().get(0).getLink());

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).sendLater(email.capture());
        assertArrayEquals(new String[]{"va@petrotrade.co.zw"}, email.getValue().getTo());
        assertTrue(email.getValue().getText().endsWith("Open it: https://fleet.example/requests/5\n"));
    }

    @Test
    void noEmailWhenNobodyHasAnAddress() {
        when(users.findByRoleInAndDepartment_Id(Role.DEPARTMENT_HEADS, 3L)).thenReturn(List.of(user(4L, " ")));
        notifications.notifyDepartmentHeads(3L, "Approval needed", "Text", "/requests/1");
        verify(inbox).saveAll(argThat(list -> list.iterator().hasNext()));
        verifyNoInteractions(mail);
    }

    @Test
    void mailGoesThroughGraphWhenConfiguredElseSmtpFromTheSender() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        GraphMailSender graph = mock(GraphMailSender.class);
        MailOutbox outbox = new MailOutbox(smtp, graph);
        ReflectionTestUtils.setField(outbox, "from", "workflow@petrotrade.co.zw");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("driver@petrotrade.co.zw");
        message.setSubject("Hello");
        message.setText("Text");

        when(graph.configured()).thenReturn(true);
        outbox.sendLater(message);
        verify(graph).send(aryEq(new String[]{"driver@petrotrade.co.zw"}), isNull(), eq("Hello"), eq("Text"));
        verifyNoInteractions(smtp);

        when(graph.configured()).thenReturn(false);
        outbox.sendLater(message);
        verify(smtp).send(argThat((SimpleMailMessage m) -> "workflow@petrotrade.co.zw".equals(m.getFrom())));
    }

    private static User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }
}
