package site.ticketlion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    private String themeColor;

    @Column(nullable = false)
    private String thumbnailEmoji;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Event(String title, LocalDateTime startAt, String venue, Integer price) {
        this.title = title;
        this.startAt = startAt;
        this.venue = venue;
        this.price = price;
    }

    public void update(String title, LocalDateTime startAt, String category, String venue,
        Integer price, String themeColor, String thumbnailEmoji) {
        this.title = title;
        this.startAt = startAt;
        this.category = category;
        this.venue = venue;
        this.price = price;
        this.themeColor = themeColor;
        this.thumbnailEmoji = thumbnailEmoji;
    }
}