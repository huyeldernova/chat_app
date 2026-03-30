package com.example.websocket.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.concurrent.TimeUnit;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@RedisHash("token")
public class RedisToken {

    @Id
    private String jwtId;

    @TimeToLive(unit = TimeUnit.MILLISECONDS)
    private Long expirationDate;
}
