package com.example.lab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PendingItemDTO {
    private Long id;
    private String type;
    private String equipmentName;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;
    private String userName;
    private String description;
}
