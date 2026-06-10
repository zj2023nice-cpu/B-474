package com.example.lab.dto;

import com.example.lab.entity.Borrow;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConflictCheckResult {
    private boolean hasConflict;
    private List<ConflictRecord> conflicts;

    @Data
    public static class ConflictRecord {
        private Long borrowId;
        private String applicantName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;

        public ConflictRecord(Borrow borrow) {
            this.borrowId = borrow.getId();
            this.applicantName = borrow.getApplicant() != null ? borrow.getApplicant().getName() : "未知";
            this.startTime = borrow.getStartTime();
            this.endTime = borrow.getEndTime();
            this.status = borrow.getStatus();
        }
    }
}
