package com.example.lab.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "labs")
public class Lab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "实验室名称不能为空")
    @Size(max = 100, message = "实验室名称长度不能超过 100 个字符")
    private String name;

    @Size(max = 50, message = "建筑名称长度不能超过 50 个字符")
    private String building;
    
    @Size(max = 20, message = "房间号长度不能超过 20 个字符")
    private String room;
    
    @Size(max = 50, message = "负责人姓名长度不能超过 50 个字符")
    private String picName; // Person In Charge
    
    @Size(max = 20, message = "负责人电话长度不能超过 20 个字符")
    private String picPhone;
    
    @Min(value = 0, message = "容量不能为负数")
    private Integer capacity;
}
