package zw.co.petrotrade.workflow.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

// Sends email through Graph when its app is configured, otherwise SMTP
@Slf4j
@Component
@RequiredArgsConstructor
public class MailOutbox {

    private final JavaMailSender smtp;
    private final GraphMailSender graph;

    // Office 365 only delivers mail from the signed-in mailbox (or one it may send as)
    @Value("${workflow.mail.from:}")
    private String from;

    public String from() {
        return from;
    }

    public String via() {
        return graph.configured() ? "Microsoft Graph" : "SMTP";
    }

    // now, on this thread; failures are thrown
    public void deliver(SimpleMailMessage message) {
        if (graph.configured()) {
            graph.send(message.getTo(), message.getCc(), message.getSubject(), message.getText());
        } else {
            message.setFrom(from);
            smtp.send(message);
        }
    }

    // in the background, so a slow mail server never holds up an approval; the in-app notification is already saved
    @Async
    public void sendLater(SimpleMailMessage message) {
        if (from.isBlank()) {
            log.debug("No sender mailbox (MAIL_FROM) is set; email not sent: {}", message.getSubject());
            return;
        }
        try {
            deliver(message);
        } catch (MailException | RestClientException e) {
            log.error("Could not send \"{}\"", message.getSubject(), e);
        }
    }
}
