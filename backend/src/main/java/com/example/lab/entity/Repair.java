package com.example.lab.entity;

import com.example.lab.enums.RepairStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "repairs")
public class Repair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "equipment_id")
    @NotNull(message = "设备不能为空")
    private Equipment equipment;

    @Size(max = 1000, message = "故障描述长度不能超过 1000 个字符")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportDate;

    @Size(max = 100, message = "维修公司名称长度不能超过 100 个字符")
    private String repairCompany;
    
    @DecimalMin(value = "0", message = "维修费用不能为负数")
    private BigDecimal cost;

    @Size(max = 1000, message = "维修结论长度不能超过 1000 个字符")
    private String repairConclusion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RepairStatus status;
    
    @ManyToOne
    @JoinColumn(name = "reporter_id")
    @NotNull(message = "报修人不能为空")
    private User reporter;
}
