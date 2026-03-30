package com.example.websocket.controller;


import com.example.websocket.dto.request.LoginRequest;
import com.example.websocket.dto.response.ApiResponse;
import com.example.websocket.dto.response.LoginResponse;
import com.example.websocket.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> authenticate(@RequestBody @Valid LoginRequest request) {
        var result = authenticationService.login(request);
        return ApiResponse.<LoginResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Login successful")
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) throws ParseException {
        String token = authHeader.substring(7);
        authenticationService.logout(token);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
    }

}
