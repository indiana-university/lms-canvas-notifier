package edu.iu.uits.lms.canvasnotifier.model;

/*-
 * #%L
 * canvasnotifier
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@Table(name = "CANVASNOTIFIER_JOB")
//@NamedQueries({
//        @NamedQuery(name = "Job.findAllRunningJobs", query = "from Job where status in ('STARTED', 'RESTARTED') order by id"),
//
//// 300 = 5 minutes in below query
//        @NamedQuery(name = "Job.getElevatedJobsOlderThan", query = "from Job job where job.senderWasElevated = true and job.senderIsElevated = true and EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) - 300 > EXTRACT(EPOCH FROM job.modifiedOn) order by job.id asc"),
//        @NamedQuery(name = "Job.getRunningJobsBySenderCanvasId", query = "from Job job where job.sender_canvasid = :senderId and ((status = 'PENDING' and EXTRACT(EPOCH FROM CURRENT_TIMESTAMP) - 300 < EXTRACT(EPOCH FROM job.modifiedOn)) or status = 'STARTED' or status = 'RESTARTED') order by job.id asc")
//})

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
    @Column(columnDefinition = "text")
    private String body;

    @NonNull
    @Lob
    @Column(columnDefinition = "text")
    private String json_csv;

    @NonNull
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "PERCENTCOMPLETED")
    private int percentCompleted;

    @Column(name = "SENDER_WAS_ELEVATED")
    private Boolean senderWasElevated;

    @Column(name = "SENDER_IS_ELEVATED")
    private Boolean senderIsElevated;

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
