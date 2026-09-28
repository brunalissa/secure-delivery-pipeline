package com.devsecops.securepipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * SecureDeliveryPipelineApplication — Spring Boot Entry Point
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>This is the main class of the application. The {@code @SpringBootApplication} annotation is a
 * composite of:
 *
 * <ul>
 *   <li>{@code @EnableAutoConfiguration} — Spring Boot auto-configures beans based on dependencies
 *       on the classpath (e.g., embeds Tomcat because spring-web is present).
 *   <li>{@code @ComponentScan} — Scans the base package ({@code com.devsecops.securepipeline}) for
 *       {@code @Controller}, {@code @Service}, {@code @Repository}, {@code @Component} classes.
 *   <li>{@code @Configuration} — This class itself can define {@code @Bean} methods.
 * </ul>
 *
 * <p>{@link SpringApplication#run} boots the application context and starts the embedded Tomcat
 * server on port 8080 (configurable via {@code server.port}).
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0 ─────────────────────────────────────────────────────────────────────────────
 */
@SpringBootApplication
public class SecureDeliveryPipelineApplication {

  /**
   * Main method — the JVM entry point.
   *
   * <p>{@code args} are command-line arguments. Spring Boot parses known arguments (those starting
   * with {@code --}) and passes them to the environment, so you can override any property at
   * runtime, e.g.: {@code java -jar app.jar --server.port=9090}
   *
   * @param args command-line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.run(SecureDeliveryPipelineApplication.class, args);
  }
}
