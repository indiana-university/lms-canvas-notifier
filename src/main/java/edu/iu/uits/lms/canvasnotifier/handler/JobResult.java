package edu.iu.uits.lms.canvasnotifier.handler;

import edu.iu.uits.lms.canvas.model.User;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import lombok.Data;
import lombok.NonNull;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

@Data
public class JobResult {
    private Long jobId;

    private Job job;

    private User canvasInitiatingUser;
    private User canvasSenderUser;

    private String senderDisplayName;

    private ProcessCounts processCounts;
    List<String> errorMessages;

    public JobResult() {
        errorMessages = new ArrayList<>();
        processCounts = new ProcessCounts();
    }

    public void addErrorMessage(@NonNull String errorMessage) {
        errorMessages.add(errorMessage);
    }

    public String getDurationOfJobString() {
        Interval interval = new Interval(this.job.getCreatedOn().getTime(), this.job.getModifiedOn().getTime());
        Period period = interval.toPeriod();

        return "| " + period.getDays() + " day(s) | " + period.getHours() + " hour(s) | " + period.getMinutes() + " minute(s) | " + period.getSeconds() + " second(s) | ";
    }

}
