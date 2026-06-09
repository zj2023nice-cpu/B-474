package com.example.lab.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinishRepairRequest {
    @NotBlank(message = "维修结论不能为空")
    @Size(max = 1000, message = "维修结论长度不能超过 1000 个字符")
    private String repairConclusion;

    @Size(max = 100, message = "维修公司名称长度不能超过 100 个字符")
    private String repairCompany;

    @DecimalMin(value = "0", message = "维修费用不能为负数")
    private BigDecimal cost;
}
