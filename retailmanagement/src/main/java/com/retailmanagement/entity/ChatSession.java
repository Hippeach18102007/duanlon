package com.retailmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false, length = 64)
    private String sessionId;

    // null = guest / unauthenticated
    @Column(name = "customer_id")
    private Integer customerId;

    // "CUSTOMER" | "STAFF"
    @Column(name = "actor_role", length = 20)
    private String actorRole;

    // JSON array of {role, content} — capped at ~50 turns to save space
    @Column(name = "history_json", columnDefinition = "TEXT")
    private String historyJson;

    @Column(name = "last_intent", length = 50)
    private String lastIntent;

    @Column(name = "escalated")
    @Builder.Default
    private Boolean escalated = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}