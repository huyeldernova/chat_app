package com.example.websocket.controller;


import com.example.websocket.dto.request.CreateUserRequest;
import com.example.websocket.dto.response.ApiResponse;
import com.example.websocket.dto.response.CreateUserResponse;
import com.example.websocket.dto.response.UserDetailResponse;
import com.example.websocket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    /**
     * POST /api/v1/users
     * Public — đăng ký tài khoản mới, đã permitAll() trong SecurityConfig
     */
    @PostMapping
    public ApiResponse<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        var result = userService.createUser(request);
        return ApiResponse.<CreateUserResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User created successfully")
                .result(result)
                .build();
    }

    /**
     * GET /api/v1/users/search?search=query
     * Cần đăng nhập — chỉ user có role USER hoặc ADMIN mới tìm được
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ApiResponse<List<UserDetailResponse>> searchUsers(
            @RequestParam("search") String search) {
        var result = userService.getUserLikeByEmailOrUsername(search);
        return ApiResponse.<List<UserDetailResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Search result")
                .result(result)
                .build();
    }
}