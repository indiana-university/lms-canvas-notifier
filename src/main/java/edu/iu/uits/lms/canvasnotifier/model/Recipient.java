package edu.iu.uits.lms.canvasnotifier.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.iu.uits.lms.common.date.DateFormatUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "CANVASNOTIFIER_RECIPIENT")
@NamedQueries({
        @NamedQuery(name = "Recipient.findProcessedForJobByUsername", query = "from Recipient where job.id = :jobId and username = :username"),
        @NamedQuery(name = "Recipient.findByJob", query = "from Recipient where job.id = :jobId order by id asc")
})

@SequenceGenerator(name = "CANVASNOTIFIER_RECIPIENT_ID_SEQ", sequenceName = "CANVASNOTIFIER_RECIPIENT_ID_SEQ", allocationSize = 1)
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"job"})
@ToString(exclude = {"job"})
@JsonIgnoreProperties("job")
public class Recipient {
    @Id
    @GeneratedValue(generator = "CANVASNOTIFIER_RECIPIENT_ID_SEQ")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(name = "FK_notifier_rcpt_job_id"), name = "notifier_job_id", nullable = false)
    private Job job;

    @NonNull
    private String username;

    @NonNull
    private String canvasid;

    private boolean successfully_processed;

    @JsonFormat(pattern = DateFormatUtil.JSON_DATE_FORMAT)
    private Date processed_date;

    private String error;

    public Recipient(String username, String canvasid) {
        this.username = username;
        this.canvasid = canvasid;
        this.successfully_processed = true;
        this.processed_date = new Date();
    }

    public Recipient(String username, String canvasid, String error) {
        this.username = username;
        this.canvasid = canvasid;
        this.successfully_processed = false;
        this.processed_date = new Date();
        this.error = error;
    }

}
