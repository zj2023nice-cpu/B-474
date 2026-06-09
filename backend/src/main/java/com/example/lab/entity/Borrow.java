package com.example.lab.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "borrows")
public class Borrow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "equipment_id")
    @NotNull(message = "设备不能为空")
    private Equipment equipment;

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    @NotNull(message = "申请人不能为空")
    private User applicant;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Size(max = 500, message = "用途说明长度不能超过 500 个字符")
    private String purpose;
    
    @Pattern(regexp = "^(PENDING|APPROVED|RETURNED|REJECTED)$", message = "状态必须是 PENDING、APPROVED、RETURNED 或 REJECTED")
    private String status; // PENDING, APPROVED, RETURNED, REJECTED

    @ManyToOne
    @JoinColumn(name = "approver_id")
    private User approver;

    @Size(max = 500, message = "拒绝原因长度不能超过 500 个字符")
    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rejectTime;
}
