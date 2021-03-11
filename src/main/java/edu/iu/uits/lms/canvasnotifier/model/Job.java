package edu.iu.uits.lms.canvasnotifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "CANVASNOTIFIER_JOB")
@NamedQueries({
        @NamedQuery(name = "Job.findAllRunningJobs", query = "from Job where status in ('STARTED', 'RESTARTED') order by id")
})

@SequenceGenerator(name = "CANVASNOTIFIER_JOB_ID_SEQ", sequenceName = "CANVASNOTIFIER_JOB_ID_SEQ", allocationSize = 1)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@ToString(exclude = {"recipients"})
@JsonIgnoreProperties({"recipients", "json_csv"})
@JsonPropertyOrder({ "id", "status", "percentCompleted" })
@JsonAppend(attrs = { @JsonAppend.Attr(value = "JsvCsvAbbrev")})
public class Job extends ModelWithDates {
    @Id
    @GeneratedValue(generator = "CANVASNOTIFIER_JOB_ID_SEQ")
    private Long id;

    @NonNull
    private String initited_by_username;

    @NonNull
    private String sender_canvasid;

    @NonNull
    private String subject;

    @NonNull
    @Lob
    @Type(type="text")
    private String body;

    @NonNull
    @Lob
    @Type(type="text")
    private String json_csv;

    @NonNull
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "PERCENTCOMPLETED")
    private int percentCompleted;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "job", orphanRemoval = true)
    private List<Recipient> recipients;

    public String getJsvCsvAbbrev() {
        final int MAX_LENGTH = 200;

        if (this.json_csv == null) {
            return "";
        } else {
            return (json_csv.length() < MAX_LENGTH) ? json_csv.substring(0, json_csv.length()) : json_csv.substring(0, MAX_LENGTH) + " ...";
        }
    }
    public void abortJob() {
        this.status = JobStatus.ABORTED;
    }

    public void failJob() {
        this.status = JobStatus.FAILED;
    }

    public void restartJob() {
        this.status = JobStatus.RESTARTED;
    }

    public void startJob() {
        this.status = JobStatus.STARTED;
    }

    public void completeJob() {
        this.status = JobStatus.FINISHED;
        this.percentCompleted = 100;
    }
}
