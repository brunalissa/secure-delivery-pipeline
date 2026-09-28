package com.devsecops.securepipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * SecureDeliveryPipelineApplicationTest — Context Load Smoke Test
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>A basic test that verifies the Spring application context loads without errors. This catches
 * configuration wiring mistakes that wouldn't be caught by unit tests alone.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0 ─────────────────────────────────────────────────────────────────────────────
 */
@org.springframework.boot.test.context.SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false"
    })
class SecureDeliveryPipelineApplicationTest {

  @org.springframework.beans.factory.annotation.Autowired
  private io.swagger.v3.oas.models.OpenAPI openApi;

  @Test
  @DisplayName("main class should exist and be instantiable")
  void shouldInstantiateApplication() {
    // Verify the class loads without exceptions
    assertThat(SecureDeliveryPipelineApplication.class).isNotNull();
    assertThat(openApi.getInfo().getTitle()).isNotBlank();
  }
}
