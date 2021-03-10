package edu.iu.uits.lms.canvasnotifier.rest;

import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessage;
import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessageSender;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.util.CanvasNotifierUtils;
//import edu.iu.uits.lms.swagger.LmsSwaggerDocumentation;
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
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
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
}
