package com.swiftpay.fraud.exception;

public class FraudProcessingException extends RuntimeException {

    public FraudProcessingException(String message) {
        super(message);
    }

    public FraudProcessingException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}