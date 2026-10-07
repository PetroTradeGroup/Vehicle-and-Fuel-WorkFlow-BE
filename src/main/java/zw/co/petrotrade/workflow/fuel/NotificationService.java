package zw.co.petrotrade.workflow.fuel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import zw.co.petrotrade.workflow.transport.TransportRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${workflow.notifications.hr-manager-email}")
    private String hrManagerEmail;

    @Value("${workflow.notifications.it-department-email}")
    private String itDepartmentEmail;

    public void notifyFuelRequestSubmitted(TransportRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(hrManagerEmail);
        message.setCc(itDepartmentEmail);
        message.setSubject("Fuel Request Approval Needed - Request #" + request.getId());
        message.setText(
                "A fuel request requires approval.\n\n"
                        + "Driver/Officer: " + request.getDriverName() + "\n"
                        + "Department: " + request.getDepartment() + "\n"
                        + "Destination: " + request.getDestination() + "\n"
                        + "Fuel required: " + request.getFuelRequiredLitres() + " litres\n");

        // the request is already saved; a mail outage must not fail the workflow step
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Could not send fuel request notification for request {}", request.getId(), e);
        }
    }
}
