package com.example.websocket.service;

import com.example.websocket.dto.request.LoginRequest;
import com.example.websocket.dto.response.LoginResponse;
import com.example.websocket.dto.token.TokenPayload;
import com.example.websocket.entity.RedisToken;
import com.example.websocket.entity.User;
import com.example.websocket.repository.RedisTokenRepository;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RedisTokenRepository redisTokenRepository;

    public LoginResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        User user = (User) authentication.getPrincipal();

        Set<String> authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        TokenPayload payloadAccessToken = jwtService.generateAccessToken(user.getId(), authorities);
        TokenPayload payloadRefreshToken = jwtService.generateRefreshToken(user.getId());

        return LoginResponse.builder()
                .accessToken(payloadAccessToken.getToken())
                .refreshToken(payloadRefreshToken.getToken())
                .build();
    }

    public void logout(String accessToken) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        Date expiredTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        Date now = new Date();

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();

        Long diff = expiredTime.getTime() - now.getTime();
        RedisToken redisToken = RedisToken.builder()
                .jwtId(jwtId)
                .expirationDate(diff)
                .build();

        redisTokenRepository.save(redisToken);
        log.info("Token saved to redis: {}", accessToken);
    }
}
