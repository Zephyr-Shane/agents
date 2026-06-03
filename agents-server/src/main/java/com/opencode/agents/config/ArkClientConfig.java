package com.opencode.agents.config;

import com.volcengine.ark.runtime.service.ArkService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "volcengine")
@Data
public class ArkClientConfig {

    private String arkApiKey;
    private String arkBaseUrl;
    private Integer timeout;
    private Integer connectTimeout;
    private Integer retryTimes;

    @Bean
    public ArkService arkService() {
        return ArkService.builder()
                .apiKey(arkApiKey)
                .baseUrl(arkBaseUrl)
                .timeout(Duration.ofSeconds(timeout))
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .retryTimes(retryTimes)
                .build();
    }

}
