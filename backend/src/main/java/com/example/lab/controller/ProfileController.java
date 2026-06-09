package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.dto.UpdateProfileRequest;
import com.example.lab.entity.User;
import com.example.lab.exception.ResourceNotFoundException;
import com.example.lab.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class ProfileController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser() {
        Long userId = getCurrentUserId();
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return ApiResponse.success(toUserResponse(user));
    }

    @PutMapping("/me")
    public ApiResponse<Map<String, Object>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            userService.updatePassword(user, request.getPassword());
        } else {
            userService.save(user);
        }

        User updatedUser = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return ApiResponse.success("更新成功", toUserResponse(updatedUser));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
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
