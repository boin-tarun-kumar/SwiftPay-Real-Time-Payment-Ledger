package com.swiftpay.fraud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI swiftPayFraudOpenAPI() {

        return new OpenAPI()
                .components(new Components())
                .info(
                        new Info()
                                .title("SwiftPay Fraud Detection API")
                                .description(
                                        "Fraud Detection Service for the " +
                                        "SwiftPay Real-Time Payment Ledger. " +
                                        "The service evaluates payment events " +
                                        "using configurable fraud rules and " +
                                        "Redis-based transaction velocity checks."
                                )
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("SwiftPay Engineering")
                                )
                                .license(
                                        new License()
                                                .name("Internal")
                                )
                );
    }
}