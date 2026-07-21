package com.doroload.api.common.config;

import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// TMAP 전용 RestClient 생성. Connect/Response Timeout은 명세서 12.4 초기 권장값을 사용한다.
@Configuration
public class TmapClientConfig {

    @Bean
    public RestClient tmapRestClient(TmapProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
                .withReadTimeout(Duration.ofSeconds(properties.responseTimeoutSeconds()));
        ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("appKey", properties.appKey())
                .build();
    }
}
