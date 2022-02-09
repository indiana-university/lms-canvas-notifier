package edu.iu.uits.lms.canvasnotifier.job;

import canvas.client.generated.api.AccountsApi;
import canvas.client.generated.api.CanvasApi;
import edu.iu.uits.lms.canvasnotifier.config.ToolConfig;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import email.client.generated.api.EmailApi;
import email.client.generated.model.EmailDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CanvasNotifierExpireElevationsService {
   @Autowired
   private AccountsApi accountsApi;

   @Autowired
   private CanvasApi canvasApi;

   @Autowired
   private EmailApi emailApi;

   @Autowired
   private JobRepository jobRepository;

   @Autowired
   private ToolConfig toolConfig;

   public void main() {
      log.info("CanvasNotifierExpireElevations job has been run");

      List<Job> jobs = jobRepository.getElevatedJobsOlderThan();

      List<String> jobIds = new ArrayList<>();

      for (Job job : jobs) {
         log.info("De-elevating user {}", job.getSender_canvasid());

         accountsApi.revokeAsAccountAdmin(canvasApi.getRootAccount(), job.getSender_canvasid());
         job.setStatus(JobStatus.FORCE_DEELEVATE);
         job.setSenderWasElevated(false);

         jobRepository.save(job);

         jobIds.add(job.getId().toString());
      }

      String resultsMessage = String.format("De-elevated %s Jobs", jobs == null || jobs.size() == 0
                                                                   ? "no" : jobs.size());

      if (jobIds.size() > 0) {
         resultsMessage += String.format(" with IDs %s", String.join(",", jobIds));

         String[] emailAddresses = toolConfig.getBatchNotificationEmail();
         String subject = emailApi.getStandardHeader() + " canvas notifier expire elevations";

         EmailDetails emailDetails = new EmailDetails();
         emailDetails.setRecipients(List.of(emailAddresses));
         emailDetails.setSubject(subject);
         emailDetails.setBody(resultsMessage);
         emailApi.sendEmail(emailDetails, true);
      }

      log.info(resultsMessage);

   }
}
