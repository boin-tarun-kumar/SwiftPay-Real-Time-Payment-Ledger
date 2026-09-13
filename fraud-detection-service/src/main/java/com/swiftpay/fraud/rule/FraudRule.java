package com.swiftpay.fraud.rule;

import com.swiftpay.common.event.PaymentInitiatedEvent;

public interface FraudRule {

    /**
     * Evaluates a payment against a single fraud rule.
     *
     * @param event payment initiation event
     * @return rule evaluation result
     */
    RuleResult evaluate(PaymentInitiatedEvent event);

    /**
     * Name of the rule.
     */
    String getRuleName();

    /**
     * Result returned by every fraud rule.
     *
     * @param triggered whether the rule was triggered
     * @param riskPoints risk points contributed by this rule
     * @param reason reason associated with the rule
     */
    record RuleResult(
            boolean triggered,
            int riskPoints,
            String reason
    ) {

        public static RuleResult triggered(
                int riskPoints,
                String reason) {

            return new RuleResult(
                    true,
                    riskPoints,
                    reason
            );
        }

        public static RuleResult notTriggered() {

            return new RuleResult(
                    false,
                    0,
                    null
            );
        }
    }
}