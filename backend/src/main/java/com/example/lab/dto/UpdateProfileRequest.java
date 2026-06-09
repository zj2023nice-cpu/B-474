package com.example.lab.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 50, message = "姓名长度不能超过 50 个字符")
    private String name;

    @Size(min = 4, max = 100, message = "密码长度应在 4-100 个字符之间")
    private String password;
}
