package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.dto.LoginRequest;
import com.example.lab.dto.ResetPasswordRequest;
import com.example.lab.entity.User;
import com.example.lab.exception.AuthenticationException;
import com.example.lab.exception.BusinessException;
import com.example.lab.exception.ResourceNotFoundException;
import com.example.lab.service.UserService;
import com.example.lab.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        logger.info("登录尝试: 用户名={}", request.getUsername());
        
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user != null) {
            logger.info("登录成功: 用户名={}, 角色={}", user.getUsername(), user.getRole());
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            Map<String, Object> response = toUserResponse(user);
            response.put("token", token);
            return ApiResponse.success("登录成功", response);
        }
        
        logger.warn("登录失败: 用户名={} - 用户名或密码错误", request.getUsername());
        throw new AuthenticationException("用户名或密码错误");
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userService.findAll();
        List<Map<String, Object>> userResponses = users.stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(userResponses);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@Valid @RequestBody User user) {
        User savedUser = userService.save(user);
        return ApiResponse.success("创建成功", toUserResponse(savedUser));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        Optional<User> existingUserOpt = userService.findById(id);
        if (existingUserOpt.isEmpty()) {
            logger.warn("更新用户失败: 用户不存在, id={}", id);
            throw new ResourceNotFoundException("用户不存在");
        }
        
        User existingUser = existingUserOpt.get();
        existingUser.setUsername(user.getUsername());
        existingUser.setRole(user.getRole());
        existingUser.setName(user.getName());
        
        User savedUser = userService.save(existingUser);
        return ApiResponse.success("更新成功", toUserResponse(savedUser));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        logger.info("管理员重置密码: 目标用户id={}", id);
        
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isEmpty()) {
            logger.warn("重置密码失败: 用户不存在, id={}", id);
            throw new ResourceNotFoundException("用户不存在");
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        
        User user = userOpt.get();
        User updatedUser = userService.updatePassword(user, request.getNewPassword());
        
        logger.info("重置密码成功: 用户id={}, 用户名={}", id, user.getUsername());
        return ApiResponse.success("密码重置成功", toUserResponse(updatedUser));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    private Map<String, Object> toUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        response.put("name", user.getName());
        return response;
    }
}
