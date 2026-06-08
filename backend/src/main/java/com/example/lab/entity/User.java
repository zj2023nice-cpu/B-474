package com.example.lab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度应在 2-50 个字符之间")
    private String username;

    @Column(nullable = false)
    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 100, message = "密码长度应在 4-100 个字符之间")
    private String password;

    @Column
    private String salt;

    @Column(nullable = false)
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(ADMIN|TEACHER|STUDENT)$", message = "角色必须是 ADMIN、TEACHER 或 STUDENT")
    private String role; // ADMIN, TEACHER, STUDENT

    @Size(max = 50, message = "姓名长度不能超过 50 个字符")
    private String name;
}
