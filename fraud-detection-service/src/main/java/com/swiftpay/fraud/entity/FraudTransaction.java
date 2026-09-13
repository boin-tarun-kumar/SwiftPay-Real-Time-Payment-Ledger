package com.swiftpay.fraud.entity;

import com.swiftpay.common.enums.FraudStatus;
import com.swiftpay.common.enums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "fraud_transactions",
        indexes = {
                @Index(
                        name = "idx_fraud_transaction_id",
                        columnList = "transaction_id",
                        unique = true
                ),
                @Index(
                        name = "idx_fraud_sender_id",
                        columnList = "sender_id"
                ),
                @Index(
                        name = "idx_fraud_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_fraud_evaluated_at",
                        columnList = "evaluated_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "transaction_id",
            nullable = false,
            unique = true,
            length = 100
    )
    private String transactionId;

    @Column(
            name = "sender_id",
            nullable = false,
            length = 100
    )
    private String senderId;

    @Column(
            name = "receiver_id",
            nullable = false,
            length = 100
    )
    private String receiverId;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(
            name = "risk_score",
            nullable = false
    )
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "risk_level",
            nullable = false,
            length = 20
    )
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private FraudStatus status;

    @Column(
            name = "reason",
            nullable = false,
            length = 500
    )
    private String reason;

    @Column(
            name = "evaluated_at",
            nullable = false
    )
    private LocalDateTime evaluatedAt;
}