package edu.iu.uits.lms.canvasnotifier.handler;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProcessCounts {
    private int successCount = 0;
    private int failureCount = 0;
    private int totalCount = 0;

    public void incrementSuccessCount() {
        this.successCount++;
        this.totalCount++;
    }

    public void incrementFailureCount() {
        this.failureCount++;
        this.totalCount++;
    }
}
