package com.retailmanagement.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "loyalty_ledger", indexes = {
        @Index(name = "IX_loyalty_customer_date", columnList = "customer_id, created_at")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LoyaltyLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull
    @Column(name = "points_delta", nullable = false)
    private Integer pointsDelta;

    @Size(max = 50)
    @Column(name = "transaction_type", length = 50)
    private String transactionType; // EARN / DEDUCT / TIER_UPGRADE / TIER_DOWNGRADE / PENALTY

    @Size(max = 30)
    @Column(name = "tier_before", length = 30)
    private String tierBefore;

    @Size(max = 30)
    @Column(name = "tier_after", length = 30)
    private String tierAfter;

    @Size(max = 200)
    @Nationalized
    @Column(name = "reason", length = 200)
    private String reason;

    @Size(max = 50)
    @Nationalized
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Size(max = 500)
    @Nationalized
    @Column(name = "note", length = 500)
    private String note;

    // ✅ CHANGED: User relationship instead of Integer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @NotNull
    @ColumnDefault("sysdatetime()")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ============================================================
    // HELPER METHODS
    // ============================================================

    public boolean isEarning() {
        return pointsDelta != null && pointsDelta > 0;
    }

    public boolean isDeduction() {
        return pointsDelta != null && pointsDelta < 0;
    }

    public boolean isTierChange() {
        return tierBefore != null || tierAfter != null;
    }

    public boolean hasReference() {
        return referenceType != null && referenceId != null;
    }

    public int getAbsolutePoints() {
        return Math.abs(pointsDelta != null ? pointsDelta : 0);
    }
}