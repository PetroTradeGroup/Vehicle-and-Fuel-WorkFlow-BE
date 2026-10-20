package zw.co.petrotrade.workflow.notification;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// One person's in-app notification: what happened, and where in the app to deal with it
@Data
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_user", columnList = "userId, id"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String title;

    @Column(length = 4000)
    private String message;

    // a page in the app, e.g. /requests/12
    private String link;

    private LocalDateTime createdAt;

    // null until they've read it
    private LocalDateTime readAt;
}
