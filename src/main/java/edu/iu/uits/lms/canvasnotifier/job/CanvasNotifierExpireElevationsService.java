package edu.iu.uits.lms.canvasnotifier.job;

import edu.iu.uits.lms.canvasnotifier.config.ToolConfig;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.service.CanvasNotifierService;
import edu.iu.uits.lms.email.model.EmailDetails;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.email.service.LmsEmailTooBigException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CanvasNotifierExpireElevationsService {
   @Autowired
   private CanvasNotifierService canvasNotifierService;

   @Autowired
   private EmailService emailService;

   @Autowired
   private JobRepository jobRepository;

   @Autowired
   private ToolConfig toolConfig;

   public void main() {
      log.info("CanvasNotifierExpireElevations job has been run");

      List<Job> jobs = jobRepository.getElevatedJobsOlderThan();

      List<String> deElevatedJobIds = new ArrayList<>();
      List<String> failedDeElevatedJobIds = new ArrayList<>();

      for (Job job : jobs) {
         log.info("De-elevating user {}", job.getSender_canvasid());

         boolean isSenderUserDeElevated = canvasNotifierService.deElevateSenderUser(job);

         if (isSenderUserDeElevated) {
            job.setStatus(JobStatus.FORCE_DEELEVATE);
            job.setSenderIsElevated(false);

            jobRepository.save(job);
            deElevatedJobIds.add(job.getId().toString());
         } else {
            log.info("Cannot de-elevate job id {} sender canvas user id {}", job.getId(), job.getSender_canvasid());
            failedDeElevatedJobIds.add(job.getId().toString());
         }
      }

      String resultsMessage = String.format("De-elevated %s Jobs", jobs == null || jobs.size() == 0
                                                                   ? "no" : jobs.size());

      if (deElevatedJobIds.size() > 0 || failedDeElevatedJobIds.size() > 0) {
         if (deElevatedJobIds.size() > 0) {
            resultsMessage += String.format(" with IDs %s", String.join(",", deElevatedJobIds));
         }

         if (failedDeElevatedJobIds.size() > 0) {
            resultsMessage += String.format(". Failed to de-elevate jobs with IDs %s.",
                    String.join(",", failedDeElevatedJobIds));
         }
         String[] emailAddresses = toolConfig.getBatchNotificationEmail();
         String subject = emailService.getStandardHeader() + " canvas notifier expire elevations";

         EmailDetails emailDetails = new EmailDetails();
         emailDetails.setRecipients(emailAddresses);
         emailDetails.setSubject(subject);
         emailDetails.setBody(resultsMessage);
         try {
            emailService.sendEmail(emailDetails, true);
         } catch (LmsEmailTooBigException | MessagingException e) {
            log.error("Error sending email", e);
         }
      }

      log.info(resultsMessage);

   }
}
