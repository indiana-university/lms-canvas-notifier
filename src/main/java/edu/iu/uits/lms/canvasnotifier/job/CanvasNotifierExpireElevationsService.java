package edu.iu.uits.lms.canvasnotifier.job;

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

import edu.iu.uits.lms.canvasnotifier.config.ToolConfig;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.service.CanvasNotifierService;
import edu.iu.uits.lms.email.model.EmailDetails;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.email.service.LmsEmailTooBigException;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

      String resultsMessage = String.format("De-elevated %s Jobs", jobs == null || jobs.isEmpty()
                                                                   ? "no" : jobs.size());

      if (!deElevatedJobIds.isEmpty() || !failedDeElevatedJobIds.isEmpty()) {
         if (!deElevatedJobIds.isEmpty()) {
            resultsMessage += String.format(" with IDs %s", String.join(",", deElevatedJobIds));
         }

         if (!failedDeElevatedJobIds.isEmpty()) {
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
            emailService.sendEmail(emailDetails);
         } catch (LmsEmailTooBigException | MessagingException e) {
            log.error("Error sending email", e);
         }
      }

      log.info(resultsMessage);

   }
}
