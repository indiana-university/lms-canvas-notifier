package edu.iu.uits.lms.canvasnotifier.rest;

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

import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessage;
import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessageSender;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.model.User;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.repository.UserRepository;
import edu.iu.uits.lms.canvasnotifier.util.CanvasNotifierUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/rest/job"})
@Slf4j
//@LmsSwaggerDocumentation
public class JobRestController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CanvasNotifierMessageSender canvasNotifierMessageSender;

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Job getJobFromId(@PathVariable Long id) {
        return jobRepository.findById(id).orElse(null);
    }

    @RequestMapping(value = "/{id}/csv", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getJobCsvFromId(@PathVariable Long id) {
        Job job = jobRepository.findById(id).orElse(null);

        if (job == null || job.getJson_csv() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cannot find csv body");
        }

        List<String[]> csvContents = CanvasNotifierUtils.deJsonCsvContent(job.getJson_csv());

        StringBuilder stringBuilder = new StringBuilder();

        for (String[] line : csvContents) {
            stringBuilder.append(String.join(",", line));
            stringBuilder.append("\r\n");
        }

        byte[] stringBytes = stringBuilder.toString().getBytes();

        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(stringBytes));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", "job_" + id + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(stringBytes.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }


    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<Job> getJobsAll() {
        List<Job> jobs = (List<Job>) jobRepository.findAll();

        // return reverse order date_started
        if (jobs != null) {
            jobs  = jobs.stream()
                    .sorted(Comparator.comparing(Job::getCreatedOn, Comparator.nullsFirst(Comparator.reverseOrder()))).collect(Collectors.toList());
        }

        return jobs;
    }

    @RequestMapping(value = "/allrunning", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<Job> getRunningJobsAll() {
        List<Job> jobs = (List<Job>) jobRepository.findAllRunningJobs();

        // return reverse order date_started
        if (jobs != null) {
            jobs  = jobs.stream()
                    .sorted(Comparator.comparing(Job::getCreatedOn, Comparator.nullsFirst(Comparator.reverseOrder()))).collect(Collectors.toList());
        }

        return jobs;
    }


    @RequestMapping(value = "/{id}/abort", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public String abortJob(@PathVariable Long id) {
        Job job = jobRepository.findById(id).orElse(null);

        if (job == null) {
            return "Job not found";
        }

        if (job.getStatus() == null) {
            return "Job has no status";
        }

        switch (job.getStatus()) {
            case ABORTED:
                return "Job is already aborted";

            case FINISHED:
                return "Cannot abort a finished job";
        }

        job.abortJob();
        jobRepository.save(job);

        return "Job ABORTED";
    }

    @RequestMapping(value = "/{id}/restart", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public String restartJob(@PathVariable Long id) {
        Job job = jobRepository.findById(id).orElse(null);

        if (job == null) {
            return "Job not found";
        }

        if (! JobStatus.ABORTED.equals(job.getStatus())) {
            return "Job #" + id + " is not in an aborted state";
        }

        job.restartJob();
        jobRepository.save(job);

        CanvasNotifierMessage canvasNotifierMessage = new CanvasNotifierMessage();
        canvasNotifierMessage.setId(id);
        canvasNotifierMessageSender.send(canvasNotifierMessage);

        return "Job #" + id + " restarted";
    }

    @RequestMapping(value = "/createphantomjobstarted", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public NotifierJobRestMessage createPhantomJobStarted(@RequestParam Long canvasSenderUserId) {
        if (canvasSenderUserId == null) {
            return new NotifierJobRestMessage("No canvas sender user id provided", null);
        }

        User user = userRepository.findByCanvasUserId(canvasSenderUserId.toString());

        if (user == null) {
            return new NotifierJobRestMessage(String.format("User not found with canvas sender user id %d provided",
                    canvasSenderUserId), null);
        }

        if (! user.isAuthorizedSender()) {
            return new NotifierJobRestMessage(String.format("Provided canvas sender user %d is not an authorized sender",
                    canvasSenderUserId), null);
        }

        Job job = new Job();
        job.setSender_canvasid(canvasSenderUserId.toString());
        job.setSubject("PHANTOM SUBJECT");
        job.setBody("PHANTOM BODY");
        job.setJson_csv("PHANTOM CSV");
        job.setStatus(JobStatus.STARTED);
        job.setInitited_by_username("REST ENDPOINT");
        job.setRecipients(new ArrayList<>());

        job = jobRepository.save(job);

        NotifierJobRestMessage notifierJobRestMessage = new NotifierJobRestMessage("Phantom job created", job);

        return notifierJobRestMessage;
    }

    @Data
    @AllArgsConstructor
    private class NotifierJobRestMessage implements Serializable {
        private String message;
        private Job job;
    }
}
