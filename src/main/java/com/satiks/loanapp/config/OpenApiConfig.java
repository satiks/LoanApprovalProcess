package com.satiks.loanapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenAPI metadata exposed by springdoc for API documentation.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates OpenAPI descriptor with high-level API metadata.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI loanApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loan Approval API")
                        .description("Backend API for loan application intake and approval workflow")
                        .version("v1")
                        .contact(new Contact().name("Loan Backend Team").email("backend@example.com")));
    }
}
