package com.example.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovalRequest {

    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "审批操作必须是 APPROVE 或 REJECT")
    private String action;

    @Size(max = 500, message = "拒绝原因长度不能超过 500 个字符")
    private String rejectReason;
}
