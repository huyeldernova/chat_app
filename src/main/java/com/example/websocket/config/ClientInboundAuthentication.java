package com.example.websocket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@RequiredArgsConstructor

@Slf4j(topic = "CLIENT-INBOUND-AUTHENTICATION")
public class ClientInboundAuthentication implements ChannelInterceptor {

    private final JwtDecoderConfiguration jwtDecoder;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if(accessor != null){
            if(StompCommand.CONNECT.equals((accessor.getCommand()))){
                String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION);
                if(authHeader != null && authHeader.startsWith("Bearer ")){
                    String token = authHeader.substring(7);

                    try{
                        Jwt jwt = jwtDecoder.decode(token);
                        String userId = jwt.getSubject();

                        List<GrantedAuthority> authorities = Optional.ofNullable(jwt.getClaimAsStringList("authorities"))
                                .orElse(Collections.emptyList())
                                .stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());


                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );

                        accessor.setUser(authentication);
                        log.info("Websocket connected");
                    }catch(Exception e){
                        log.error("Invalid token: {}", e.getMessage());
                        throw new MessagingException("Invalid token!");
                    }
                }
            }
        }

        return message;
    }
}
