package com.example.lab.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "equipments")
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "设备编码不能为空")
    @Size(max = 20, message = "设备编码长度不能超过 20 个字符")
    private String code; // LABxx-xxx logic

    @Size(max = 100, message = "设备名称长度不能超过 100 个字符")
    private String name;
    
    @Size(max = 50, message = "型号长度不能超过 50 个字符")
    private String model;
    
    @Size(max = 100, message = "制造商长度不能超过 100 个字符")
    private String manufacturer;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "lab_id")
    private Lab lab;

    @Pattern(regexp = "^(NORMAL|BORROWED|REPAIRING|SCRAPPED)$", message = "状态必须是 NORMAL、BORROWED、REPAIRING 或 SCRAPPED")
    private String status; // NORMAL, BORROWED, REPAIRING, SCRAPPED

    @Min(value = 0, message = "使用寿命不能为负数")
    private Integer lifeSpan; // in years
}
