package at.big5health.klimaatlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;   // Already on CacheConfig
import org.springframework.scheduling.annotation.EnableScheduling; // Already on CacheConfig

/**
 * Main application class for the Klimaatlas backend service.
 * <p>
 * This class serves as the entry point for the Spring Boot application.
 * The {@link SpringBootApplication @SpringBootApplication} annotation enables:
 * <ul>
 *   <li>{@link org.springframework.boot.autoconfigure.EnableAutoConfiguration @EnableAutoConfiguration}:
 *       Automatically configures the Spring application based on JAR dependencies.</li>
 *   <li>{@link org.springframework.context.annotation.ComponentScan @ComponentScan}:
 *       Scans for Spring components (e.g., {@code @Service}, {@code @RestController})
 *       in the current package ({@code at.big5health.klimaatlas}) and its sub-packages.</li>
 *   <li>{@link org.springframework.context.annotation.Configuration @Configuration}:
 *       Allows registering extra beans in the context or importing additional
 *       configuration classes.</li>
 * </ul>
 * Caching ({@link EnableCaching @EnableCaching}) and Scheduling
 * ({@link EnableScheduling @EnableScheduling}) are also enabled here, though they are
 * more specifically configured in {@link CacheConfig}. It's common to have these
 * annotations at the main application class level as well for global enablement.
 *
 * @see SpringBootApplication
 * @see CacheConfig
 */
@SpringBootApplication
@EnableCaching   // Often redundant if also on a specific @Configuration class like CacheConfig, but harmless.
@EnableScheduling // Often redundant if also on a specific @Configuration class like CacheConfig, but harmless.
public class KlimaatlasApplication {

	/**
	 * The main method which serves as the entry point for the Spring Boot application.
	 *
	 * @param args Command-line arguments passed to the application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(KlimaatlasApplication.class, args);
	}
}
