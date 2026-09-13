package com.swiftpay.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.enums.CurrencyType;
import com.swiftpay.common.enums.PaymentStatus;
import com.swiftpay.gateway.dto.PaymentRequest;
import com.swiftpay.gateway.dto.PaymentResponse;
import com.swiftpay.gateway.service.PaymentService;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("removal")
@MockBean
    private PaymentService paymentService;

    @SuppressWarnings("null")
@Test
    void shouldCreatePaymentSuccessfully()
            throws Exception {

        PaymentRequest request =
                new PaymentRequest();

        request.setTransactionId("TXN1001");
        request.setSenderId(101L);
        request.setReceiverId(102L);
        request.setAmount(
                BigDecimal.valueOf(500));
        request.setCurrency(
                CurrencyType.INR);

        PaymentResponse response =
                PaymentResponse.builder()
                        .transactionId("TXN1001")
                        .status(
                                PaymentStatus.PENDING)
                        .message(
                                "Payment accepted")
                        .build();

        Mockito.when(
                        paymentService.processPayment(
                                Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/v1/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request)))
                .andExpect(
                        status().isAccepted())
                .andExpect(
                        jsonPath(
                                "$.transactionId")
                                .value("TXN1001"));
    }
}