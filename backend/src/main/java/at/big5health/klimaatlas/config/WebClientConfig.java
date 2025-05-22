package at.big5health.klimaatlas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class that provides a {@link WebClient} bean for making reactive HTTP requests.
 * <p>
 * This configuration allows the {@link WebClient} to be injected wherever needed within the Spring application.
 */
@Configuration
public class WebClientConfig {

    /**
     * Creates and provides a default {@link WebClient} instance.
     * <p>
     * This bean can be injected into services or components that require making HTTP requests.
     *
     * @return a configured {@link WebClient} instance
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

}
