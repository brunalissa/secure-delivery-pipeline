package com.devsecops.securepipeline.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * OpenApiConfig — Swagger / OpenAPI 3 Documentation Configuration
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>Configures the OpenAPI 3 specification that Swagger UI renders at
 * {@code /swagger-ui.html}. This provides:
 * <ul>
 *   <li>Interactive API documentation</li>
 *   <li>Request/response schema examples</li>
 *   <li>Try-it-out functionality for each endpoint</li>
 * </ul>
 *
 * <p>In a production deployment, you might disable Swagger UI for security reasons.
 * The {@code @Profile("!prod")} annotation is an example of how to do that.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Configuration
public class OpenApiConfig {

    /**
     * Defines the OpenAPI metadata bean.
     * This information appears at the top of the Swagger UI page.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Secure Delivery Pipeline API")
                .version("1.0.0")
                .description("A RESTful API demonstrating DevSecOps best practices")
                .contact(new Contact()
                    .name("DevSecOps Team")
                    .email("team@devsecops.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")));
    }
}