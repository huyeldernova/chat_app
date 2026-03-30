package com.example.websocket.service;

import com.example.websocket.common.TokenType;
import com.example.websocket.dto.response.TokenVerificationResponse;
import com.example.websocket.dto.token.JwtInfo;
import com.example.websocket.dto.token.TokenPayload;
import com.example.websocket.entity.RedisToken;
import com.example.websocket.exception.AppException;
import com.example.websocket.exception.ErrorCode;
import com.example.websocket.repository.RedisTokenRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.example.websocket.constant.AppConstant.AUTHORITIES;
import static com.example.websocket.constant.AppConstant.TOKEN_TYPE;


@Service
@Slf4j(topic = "JWT-SERVICE")
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private final RedisTokenRepository redisTokenRepository;

    public TokenPayload generateAccessToken(String userId, Set<String> authorities) {
        JWSAlgorithm algorithm = JWSAlgorithm.HS512;
        JWSHeader header = new JWSHeader(algorithm);

        Date issueTime = new Date();
        Date expiredTime = new Date(Instant.now().plus(120, ChronoUnit.MINUTES).toEpochMilli());

        String jwtId = UUID.randomUUID().toString();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(issueTime)
                .expirationTime(expiredTime)
                .jwtID(jwtId)
                .claim(TOKEN_TYPE, TokenType.ACCESS_TOKEN.name())
                .claim(AUTHORITIES, authorities)
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            log.error("Generate accessToken error: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }

        String token = jwsObject.serialize();
        return TokenPayload.builder()
                .jwtId(jwtId)
                .token(token)
                .issueTime(issueTime)
                .expiredTime(expiredTime)
                .build();
    }

    public TokenPayload generateRefreshToken(String userId) {
        JWSAlgorithm algorithm = JWSAlgorithm.HS512;
        JWSHeader header = new JWSHeader(algorithm);

        Date issueTime = new Date();
        Date expiredTime = new Date(Instant.now().plus(14, ChronoUnit.DAYS).toEpochMilli());

        String jwtId = UUID.randomUUID().toString();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(issueTime)
                .expirationTime(expiredTime)
                .jwtID(jwtId)
                .claim(TOKEN_TYPE, TokenType.REFRESH_TOKEN.name())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            log.error("Generate refreshToken error: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }

        String token = jwsObject.serialize();
        return TokenPayload.builder()
                .jwtId(jwtId)
                .token(token)
                .issueTime(issueTime)
                .expiredTime(expiredTime)
                .build();
    }

    public JwtInfo parseToken(String token) throws ParseException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Date issueTime = signedJWT.getJWTClaimsSet().getIssueTime();
        Date expiredTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        return JwtInfo.builder()
                .jwtId(jwtId)
                .issueTime(issueTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .expirationTime(expiredTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }

    public TokenVerificationResponse verifyToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        TokenType tokenType = TokenType.valueOf((String) signedJWT.getJWTClaimsSet().getClaim(TOKEN_TYPE));

        if(tokenType != TokenType.ACCESS_TOKEN) {
            log.error("Invalid token type: {}", tokenType);
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }

        Date expiredTime  = signedJWT.getJWTClaimsSet().getExpirationTime();
        if(expiredTime.before(new Date())) {
            log.error("Token expired");
            return TokenVerificationResponse.builder()
                    .isValid(false)
                    .build();
        }

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Optional<RedisToken> redisToken = redisTokenRepository.findById(jwtId);
        if(redisToken.isPresent()) {
            log.error("Token is blacklisted. JWT ID: {}", jwtId);
            return TokenVerificationResponse.builder()
                    .isValid(false)
                    .build();
        }
        boolean isValid = signedJWT.verify(new MACVerifier(secretKey));
        Object claim =  signedJWT.getJWTClaimsSet().getClaim(AUTHORITIES);
        List<String> authorities = extractAuthorities(claim);

        return TokenVerificationResponse.builder()
                .isValid(isValid)
                .authorities(authorities)
                .build();

    }

    private List<String> extractAuthorities(Object authorities) {
        if(authorities == null) {
            return new ArrayList<>();
        }

        if(authorities instanceof List<?> authoritiesList) {
            return authoritiesList.stream()
                    .map(String::valueOf)
                    .toList();
        }
        return Collections.emptyList();
    }

}
