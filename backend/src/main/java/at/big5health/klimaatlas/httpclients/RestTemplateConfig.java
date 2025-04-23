package at.big5health.klimaatlas.httpclients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration.
 * <p>
 *     This class is used to send HTTP Client Requests.
 * </p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a new RestTemplate object.
     *
     * @return a new RestTemplate object.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
