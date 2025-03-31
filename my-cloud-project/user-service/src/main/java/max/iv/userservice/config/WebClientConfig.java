package max.iv.userservice.config;

import max.iv.userservice.client.CompanyServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;

@Configuration
public class WebClientConfig {
    @Value("${services.company.name:company-service}")
    private String companyServiceName;
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
    @Bean
    public CompanyServiceClient companyServiceClient(WebClient.Builder webClientBuilder) {
        WebClient webClient = webClientBuilder
                .baseUrl("https://" + companyServiceName)
                .build();
        WebClientAdapter adapter = WebClientAdapter.create(webClient);

        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .build();

        return factory.createClient(CompanyServiceClient.class);
    }

}
