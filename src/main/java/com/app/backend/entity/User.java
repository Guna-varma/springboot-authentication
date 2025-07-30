package com.app.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_role_id", columnList = "role_id"),
        @Index(name = "idx_users_enabled", columnList = "enabled"),
        @Index(name = "idx_users_provider", columnList = "provider"),
        @Index(name = "idx_users_email_enabled", columnList = "email, enabled"),
        @Index(name = "idx_users_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String fullName;

    @Column(unique = true, nullable = false, length = 100) // Added length constraint
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) // Added length constraint
    private Gender gender;

    @Column(nullable = true, length = 15) // Added length constraint
    private String phoneNumber;

    @Column(nullable = true, length = 255) // Added length constraint for hashed password
    @JsonIgnore
    private String password;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10) // Added length constraint
    private AuthProvider provider;

    @ManyToOne(fetch = FetchType.LAZY) // Changed to LAZY for performance
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // Changed to LocalDateTime for better performance

    @Column(nullable = false)
    private LocalDateTime updatedAt; // Changed to LocalDateTime for better performance

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}




//package com.app.backend.entity;
//
//
//import jakarta.persistence.*;
//import lombok.*;
//import java.util.Date;
//
//@Entity
//@Table(name = "users")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class User {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, length = 50)
//    private String fullName;
//
//    @Column(unique = true, nullable = false)
//    private String email;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private Gender gender;
//
//
////    @Column(nullable = true, length = 6)
////    private String gender;
//
////    @Column(length = 10)
////    private String phoneNumber;
//
//    @Column(nullable = true)
//    private String phoneNumber;
//
//    @Column(nullable = true)
//    private String password;  // Ensure to save hashed password only
//
//    private boolean enabled;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private AuthProvider provider;
//
//    @ManyToOne(fetch = FetchType.EAGER)
//    @JoinColumn(name = "role_id")
//    private Role role;
//
//    @Column(nullable = false, updatable = false)
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date createdAt;
//
//    @Column(nullable = false)
//    @Temporal(TemporalType.TIMESTAMP)
//    private Date updatedAt;
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = new Date();
//        updatedAt = new Date();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = new Date();
//    }
//}
