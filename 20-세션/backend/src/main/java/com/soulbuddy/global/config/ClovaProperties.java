package com.soulbuddy.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "clova")
public class ClovaProperties {

    private String apiKey;
    private String host;
    private int timeoutSeconds = 30;
    private int maxRetries = 2;
    private RequestId requestId = new RequestId();
    private Endpoint endpoint = new Endpoint();

    @Getter
    @Setter
    public static class RequestId {
        private String personaFriend;
        private String personaCounselor;
        private String classifyEmotion;
        private String classifyRisk;
        private String classifyIntervention;
        private String summary;
    }

    @Getter
    @Setter
    public static class Endpoint {
        private String personaFriend;
        private String personaCounselor;
        private String summary;
        private String classifyEmotion;
        private String classifyRisk;
        private String classifyIntervention;
    }
}
