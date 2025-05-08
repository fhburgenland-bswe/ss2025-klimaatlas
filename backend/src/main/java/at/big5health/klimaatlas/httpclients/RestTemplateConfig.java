package at.big5health.klimaatlas.httpclients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration class for {@link RestTemplate}.
 * <p>
 * This class provides a bean definition for a standard {@link RestTemplate}.
 * {@link RestTemplate} is a synchronous HTTP client, part of Spring Framework,
 * used for making RESTful API calls.
 * <p>
 * Note: For new development, especially in reactive applications, consider using
 * {@link org.springframework.web.reactive.function.client.WebClient WebClient}
 * for non-blocking, reactive HTTP requests. This configuration may be for legacy
 * components or specific use cases requiring a synchronous client.
 *
 * @see RestTemplate
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates and provides a singleton bean of {@link RestTemplate}.
     * <p>
     * This default {@link RestTemplate} can be injected into other components
     * for making synchronous HTTP requests.
     *
     * @return A new {@link RestTemplate} instance.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
