package com.example.routeoptimizer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Route and Shift Assignment Optimiser API")
                        .version("1.0.0")
                        .description("Backend v1.0 — Single source of truth for technician scheduling, hard-rule validation, greedy insertion optimization, local search, manual overrides, and sick technician redistribution.")
                        .contact(new Contact()
                                .name("Engineering Team")
                                .email("engineering@example.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
