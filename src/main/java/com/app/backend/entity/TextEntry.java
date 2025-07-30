package com.app.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "text_entry",
        indexes = {
                @Index(name = "idx_owner", columnList = "owner"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_message", columnList = "message"),
                @Index(name = "idx_created_at", columnList = "createdAt"),
                @Index(name = "idx_updated_at", columnList = "updatedAt")
        }
)
public class TextEntry implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Message must not be blank")
    @Size(max = 2048, message = "Message is too long")
    @Column(nullable = false, length = 2048)
    private String message;

    @NotBlank(message = "Owner must be specified")
    @Size(max = 256, message = "Owner is too long")
    @Column(nullable = false, length = 256)
    private String owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
