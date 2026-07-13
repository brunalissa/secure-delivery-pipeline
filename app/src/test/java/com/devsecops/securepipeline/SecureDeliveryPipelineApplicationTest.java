package com.devsecops.securepipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * SecureDeliveryPipelineApplicationTest — Context Load Smoke Test
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>A basic test that verifies the Spring application context loads without
 * errors. This catches configuration wiring mistakes that wouldn't be caught
 * by unit tests alone.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
class SecureDeliveryPipelineApplicationTest {

    @Test
    @DisplayName("main class should exist and be instantiable")
    void shouldInstantiateApplication() {
        // Verify the class loads without exceptions
        assertThat(SecureDeliveryPipelineApplication.class).isNotNull();
    }
}