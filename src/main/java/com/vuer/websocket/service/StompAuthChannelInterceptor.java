package com.vuer.websocket.interceptor;

import com.vuer.auth.service.JwtService;
import com.vuer.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * The mobile app sends "Authorization: Bearer <jwt>" as a STOMP connectHeader on CONNECT.
 * Spring's WebSocket support does NOT read that automatically - without this interceptor,
 * every STOMP session is anonymous, so SimpMessagingTemplate#convertAndSendToUser(userId, ...)
 * has no session to route to and silently drops the message.
 *
 * This interceptor reads that header on CONNECT, validates the JWT the same way JwtAuthFilter
 * does for REST calls, and attaches a Principal whose name is the user's id (a UUID string) -
 * that exact value is what MessageService passes as the first argument to convertAndSendToUser,
 * so the two must match.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String username = jwtService.extractUsername(token);
                    User user = (User) userDetailsService.loadUserByUsername(username);

                    if (jwtService.isTokenValid(token, user.getUsername())) {
                        Principal principal = new UsernamePasswordAuthenticationToken(
                                user.getId().toString(), null, user.getAuthorities());
                        accessor.setUser(principal);
                    } else {
                        log.warn("Rejected STOMP CONNECT: JWT failed validation");
                    }
                } catch (Exception e) {
                    log.warn("Rejected STOMP CONNECT: could not authenticate token ({})", e.getMessage());
                }
            } else {
                log.warn("STOMP CONNECT with no Authorization header - session will not receive user-targeted messages");
            }
        }

        return message;
    }
}