package com.swiftpay.fraud.dto;

import com.swiftpay.common.enums.FraudStatus;
import com.swiftpay.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResponse {

    private String transactionId;

    private FraudStatus status;

    private Integer riskScore;

    private RiskLevel riskLevel;

    private String reason;
}