package com.example.websocket.service;

import com.example.websocket.dto.request.CreateUserRequest;
import com.example.websocket.dto.response.CreateUserResponse;
import com.example.websocket.dto.response.UserDetailResponse;
import com.example.websocket.entity.Role;
import com.example.websocket.entity.User;
import com.example.websocket.exception.AppException;
import com.example.websocket.exception.ErrorCode;
import com.example.websocket.repository.RoleRepository;
import com.example.websocket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.websocket.constant.AppConstant.USER_ROLE;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(rollbackFor = Exception.class)
    public CreateUserResponse createUser(CreateUserRequest request) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        Role role = roleRepository.findByName(USER_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(USER_ROLE)
                        .build()));
        user.addRole(role);

        userRepository.save(user);

        return CreateUserResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    public List<UserDetailResponse> getUserLikeByEmailOrUsername(String emailOrUsername){
        var users = userRepository.searchByEmailOrUsername(emailOrUsername);

        return users.stream().map(u -> {
            var roles = u.getUserHasRoles().stream()
                    .map(ur -> ur.getRole().getName())
                    .toList();

            return UserDetailResponse.builder()
                    .id(u.getId())
                    .username(u.getUsername())
                    .email(u.getEmail())
                    .roles(roles)
                    .build();
        }).toList();
    }

}