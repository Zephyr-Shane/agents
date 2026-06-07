package com.opencode.agents.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wechat.miniapp")
@Data
public class WeChatConfig {

    private String appId;
    private String appSecret;
    private String loginUrl;

}
