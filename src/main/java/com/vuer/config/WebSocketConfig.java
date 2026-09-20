package com.vuer.config;

import com.vuer.websocket.interceptor.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // No .withSockJS() here - the app connects with a plain native
        // WebSocket via @stomp/stompjs's brokerURL, not through SockJS's
        // own handshake protocol. SockJS exists for browsers without native
        // WebSocket support, which doesn't apply here, and having it enabled
        // caused the server to reject every raw upgrade request to /ws with
        // "400 Bad Request" instead of accepting it - the two were never
        // actually compatible.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Without this, CONNECT frames are never authenticated and
        // convertAndSendToUser(...) has no session to deliver to.
        registration.interceptors(stompAuthChannelInterceptor);
    }
}