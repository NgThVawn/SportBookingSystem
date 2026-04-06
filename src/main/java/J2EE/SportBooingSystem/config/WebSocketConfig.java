package J2EE.SportBooingSystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker nhận message từ server để đẩy tới client
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix cho các @MessageMapping trong controller
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint client kết nối: ws://host/ws  hoặc fallback qua SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Điều chỉnh origin khi deploy production
                .withSockJS();
    }
}