package com.swiftpay.fraud.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    /*
     * Service
     */
    public static final String SERVICE_NAME =
            "swiftpay-fraud-detection-service";

    public static final String API_VERSION =
            "/api/v1";

    /*
     * Kafka topics
     */
    public static final String PAYMENT_INITIATED_TOPIC =
            "payment-initiated";

    public static final String FRAUD_CHECK_RESULT_TOPIC =
            "fraud-check-result";

    /*
     * Kafka consumer
     */
    public static final String FRAUD_CONSUMER_GROUP =
            "fraud-detection-group";

    /*
     * Redis key prefixes
     */
    public static final String FRAUD_VELOCITY_KEY_PREFIX =
            "fraud:velocity:sender:";

    public static final String FRAUD_RECEIVER_KEY_PREFIX =
            "fraud:receivers:";

    public static final String FRAUD_TRANSACTION_STATUS_PREFIX =
            "fraud:transaction:";

    /*
     * Redis expiration
     */
    public static final long VELOCITY_WINDOW_SECONDS =
            60L;

    public static final long RECEIVER_HISTORY_DAYS =
            30L;

    /*
     * Fraud scoring
     */
    public static final int HIGH_RISK_SCORE =
            70;

    public static final int MEDIUM_RISK_SCORE =
            40;

    public static final int MAX_RISK_SCORE =
            100;

    /*
     * Fraud rule points
     */
    public static final int HIGH_AMOUNT_RISK_POINTS =
            50;

    public static final int MEDIUM_AMOUNT_RISK_POINTS =
            25;

    public static final int HIGH_VELOCITY_RISK_POINTS =
            40;

    public static final int MEDIUM_VELOCITY_RISK_POINTS =
            20;

    public static final int NEW_RECEIVER_RISK_POINTS =
            15;

    /*
     * Default amount thresholds.
     */
    public static final String DEFAULT_HIGH_AMOUNT_THRESHOLD =
            "100000";

    public static final String DEFAULT_MEDIUM_AMOUNT_THRESHOLD =
            "50000";

    /*
     * Velocity thresholds.
     */
    public static final long HIGH_VELOCITY_THRESHOLD =
            5L;

    public static final long MEDIUM_VELOCITY_THRESHOLD =
            3L;
}