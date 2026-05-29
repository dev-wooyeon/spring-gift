package gift.auth.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuration to boot up Spring Declarative HTTP clients using RestClient as the underlying client.
 *
 * @author brian.kim
 * @since 1.0
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public KakaoApi kakaoApi(RestClient.Builder restClientBuilder) {
        RestClient restClient = restClientBuilder.build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        
        return factory.createClient(KakaoApi.class);
    }
}
