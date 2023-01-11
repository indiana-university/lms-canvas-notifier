package edu.iu.uits.lms.canvasnotifier.handler;

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

import edu.iu.uits.lms.canvas.model.Conversation;
import edu.iu.uits.lms.canvas.model.ConversationCreateWrapper;
import edu.iu.uits.lms.canvas.model.User;
import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.ConversationService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessage;
import edu.iu.uits.lms.canvasnotifier.config.ToolConfig;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.model.Recipient;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.repository.RecipientRepository;
import edu.iu.uits.lms.canvasnotifier.repository.UserRepository;
import edu.iu.uits.lms.canvasnotifier.service.CanvasNotifierService;
import edu.iu.uits.lms.canvasnotifier.util.CanvasNotifierUtils;
import edu.iu.uits.lms.common.date.DateFormatUtil;
import edu.iu.uits.lms.email.model.EmailDetails;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.iuonly.model.ListWrapper;
import edu.iu.uits.lms.iuonly.services.CanvasDataServiceImpl;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NotificationMessageHandler {
    @Autowired
    private AccountService accountService;

    @Autowired
    private CanvasService canvasService;

    @Autowired
    private CanvasDataServiceImpl canvasDataService;

    @Autowired
    private CanvasNotifierService canvasNotifierService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RecipientRepository recipientRepository;

    @Autowired
    private ToolConfig toolConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    public boolean handleMessage(CanvasNotifierMessage canvasNotifierMessage) {
        JobResult jobResult = new JobResult();
        jobResult.setJobId(canvasNotifierMessage.getId());

        validateJob(jobResult);

        // validate things up to this point
        if (emailIfReady(jobResult)) {
            return true;
        }

        try {
            return processJob(jobResult);
        } catch (Exception e) {
            final String error = "Job # " + jobResult.getJob().getId() + " aborted due to " + e;

            log.error(error, e);
            jobResult.addErrorMessage(error);
            jobResult.getJob().failJob();
            saveJob(jobResult);
            emailIfReady(jobResult);
        }

        return true;
    }

    private boolean processJob(@NonNull JobResult jobResult) throws Exception {
        List<String[]> csvContents = CanvasNotifierUtils.deJsonCsvContent(jobResult.getJob().getJson_csv());

        List<String> recipientsListForCanvasData = getRecipientsListForCanvasData(csvContents);

        Map<String, String> usernameToCanvasidMap = null;

        log.info("Fetching canvas data for " + recipientsListForCanvasData.size() + " usernames");

        try {
            ListWrapper listWrapper = new ListWrapper();
            listWrapper.setListItems(recipientsListForCanvasData);
            usernameToCanvasidMap = canvasDataService.getActiveUserMapOfIuUsernameToCanvasId(listWrapper);
        } catch (Exception e) {
            log.error("uh oh", e);
        }

        log.info("Fetched " + usernameToCanvasidMap.size() + " canvas ids from canvas data");

        // this is so in case the sending user was already an admin, after the job we don't de-elevate them
        boolean wasAccountAdminBeforeJobRun = accountService.isAccountAdmin(canvasService.getRootAccount(), String.valueOf(jobResult.getCanvasSenderUser().getId()));

        if (wasAccountAdminBeforeJobRun) {
            jobResult.getJob().setSenderWasElevated(false);
        } else {
            jobResult.getJob().setSenderWasElevated(true);
            jobResult.getJob().setSenderIsElevated(true);

            // elevate sender user
            if (! accountService.elevateToAccountAdmin(canvasService.getRootAccount(), String.valueOf(jobResult.getCanvasSenderUser().getId()))) {
                jobResult.addErrorMessage("cannot elevate sending user " + jobResult.getCanvasSenderUser().getId());
            }

            log.info("Sender {} was elevated", jobResult.getSenderDisplayName() +
                                               " (" + jobResult.getCanvasSenderUser().getId() + ")");
        }

        saveJob(jobResult);

        // validate things up to this point
        if (emailIfReady(jobResult)) {
            return true;
        }

        switch (jobResult.getJob().getStatus()) {
            case PENDING:
                jobResult.getJob().startJob();
                break;
            case FINISHED:
                return true;
            default:
                jobResult.getJob().restartJob();
                break;
        }

        saveJob(jobResult);

        // in case of a restart
        computeCountsFromDatabase(jobResult);

        // *****************************************************************
        // ** All is ready - let's process all the recipients
        // *****************************************************************

        final int totalRecordsForDisplay = csvContents.size() - 1;

        // start at 1 because 0 is the header line
        for (int index = 1; index < csvContents.size(); index++) {
            if (isAbortJob(jobResult.getJob().getId())) {
                jobResult.getJob().abortJob();
                jobResult.addErrorMessage("Job aborted");
                saveJob(jobResult);
                emailIfReady(jobResult);
                log.info("*** ABORTED Job #" + jobResult.getJob().getId());

                if (! wasAccountAdminBeforeJobRun) {
                    accountService.revokeAsAccountAdmin(canvasService.getRootAccount(), String.valueOf(jobResult.getCanvasSenderUser().getId()));
                }

                return true;
            }

            Map<String, String> lineMappedContents = CanvasNotifierUtils.createCsvLineDataMap(csvContents, index);

            String canvasRecipientUsername = lineMappedContents.get(CanvasNotifierUtils.USERNAME_COLUMN_NAME);

            if (canvasRecipientUsername == null) {
                // most likely a blank line (usually at the end of the file). Skip processing it
                continue;
            }

            if (JobStatus.RESTARTED.equals(jobResult.getJob().getStatus()) && recipientRepository.findProcessedForJobByUsername(jobResult.getJobId(), canvasRecipientUsername) != null) {
                // we already processed this entry (most likely an interrupted job re-started)

                continue;
            }

            String canvasRecipientUserId = getCanvasUserIdFromUsername(canvasRecipientUsername, usernameToCanvasidMap);

            if (canvasRecipientUserId == null) {
                String errorMessage = "Recipient with username " + canvasRecipientUsername + " canvas user id not found";
                jobResult.addErrorMessage(errorMessage);

                jobResult.getProcessCounts().incrementFailureCount();

                Recipient newRecipient = new Recipient(canvasRecipientUsername, canvasRecipientUserId, errorMessage);
                newRecipient.setJob(jobResult.getJob());

                recipientRepository.save(newRecipient);
                saveJob(jobResult);

                continue;

            }

            String newBody = CanvasNotifierUtils.getVariableReplacedBody(lineMappedContents, jobResult.getJob().getBody());

            if (newBody == null || newBody.isEmpty()) {
                String errorMessage = "Body is empty for username/canvasId = " + canvasRecipientUsername + "/" + canvasRecipientUserId;
                jobResult.addErrorMessage(errorMessage);

                jobResult.getProcessCounts().incrementFailureCount();

                Recipient newRecipient = new Recipient(canvasRecipientUsername, canvasRecipientUserId, errorMessage);
                newRecipient.setJob(jobResult.getJob());

                recipientRepository.save(newRecipient);
                saveJob(jobResult);

                continue;
            }

            // update percent completed
            float percent = (float) index / (float) totalRecordsForDisplay;

            jobResult.getJob().setPercentCompleted((int) (percent * 100));

            // send conversation
            ConversationCreateWrapper conversationCreateWrapper = new ConversationCreateWrapper();
            conversationCreateWrapper.setRecipients(new String[] {canvasRecipientUserId});
            conversationCreateWrapper.setSubject(jobResult.getJob().getSubject());
            conversationCreateWrapper.setBody(newBody);
            conversationCreateWrapper.setContextCode(jobResult.getJob().getSubject());
            conversationCreateWrapper.setGroupConversation(false);

            // Add try/catch so that the whole job doesn't abort if there's a problem
            Conversation createConversation = null;
            try {
                createConversation = conversationService.postConversation(conversationCreateWrapper, jobResult.getCanvasSenderUser().getId(), false);
            } catch (RestClientException rce) {
                log.error("Posting conversation failed for " + canvasRecipientUsername, rce);
            }

            if (createConversation == null) {
                String errorMessage = "Error sending conversation for username/canvasId = " + canvasRecipientUsername + "/" + canvasRecipientUserId;
                jobResult.addErrorMessage(errorMessage);
                jobResult.getProcessCounts().incrementFailureCount();

                Recipient newRecipient = new Recipient(canvasRecipientUsername, canvasRecipientUserId, errorMessage);
                newRecipient.setJob(jobResult.getJob());

                recipientRepository.save(newRecipient);
                saveJob(jobResult);

            } else { // everything went well - record processed successfully
                log.info(index + " / " + totalRecordsForDisplay + " - Successfully processed username " + canvasRecipientUsername);
                jobResult.getProcessCounts().incrementSuccessCount();

                Recipient newRecipient = new Recipient(canvasRecipientUsername, canvasRecipientUserId);
                newRecipient.setJob(jobResult.getJob());

                recipientRepository.save(newRecipient);
                saveJob(jobResult);
            }

        }

        jobResult.getJob().completeJob();
        saveJob(jobResult);

        log.debug("processing job #" + jobResult.getJob().getId() + " done.");

        // since finish date is set, this will for sure email out a full summary
        emailIfReady(jobResult);

        if (! wasAccountAdminBeforeJobRun) {
            boolean isSenderUserDeElevated = canvasNotifierService.deElevateSenderUser(jobResult.getJob());

            if (isSenderUserDeElevated) {
                log.info("Sender {} was DE-elevated", jobResult.getSenderDisplayName() +
                        " (" + jobResult.getCanvasSenderUser().getId() + ")");

                jobResult.getJob().setSenderIsElevated(false);
                saveJob(jobResult);

            } else {
                log.info("Sender {} could NOT be DE-elevated", jobResult.getJob().getSender_canvasid());
            }
        }

        return true;
    }

    public void validateJob (@NonNull JobResult jobResult) {
        Long jobId = jobResult.getJobId();

        if (jobId == null) {
            String errorMessage = "JobId can't be empty";

            jobResult.addErrorMessage(errorMessage);
            log.error(errorMessage);
            return;
        }

        Job job = jobRepository.findById(jobId).orElse(null);

        if (job == null) {
            String errorMessage = "Can't find jobId";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        if (job.getJson_csv() == null) {
            String errorMessage = "No csv data found";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        if (job.getSubject() == null) {
            String errorMessage = "No subject found";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        if (job.getBody() == null) {
            String errorMessage = "No body found";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        if (job.getInitited_by_username() == null) {
            String errorMessage = "No initiated by user found";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        User canvasInitiatingUser = userService.getUserBySisLoginId(job.getInitited_by_username());

        if (canvasInitiatingUser == null) {
            String errorMessage = "Initiated by user not found in canvas";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        jobResult.setCanvasInitiatingUser(canvasInitiatingUser);

        if (userRepository.findByUsername(job.getInitited_by_username()) == null) {
            String errorMessage = "Initiated by user not found in notifier database";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        edu.iu.uits.lms.canvasnotifier.model.User notifierInitiatedUser = userRepository.findByUsername(job.getInitited_by_username());

        if (! notifierInitiatedUser.isAuthorizedUser()) {
            String errorMessage = "Initiated by user is not an authorized user";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        if (job.getSender_canvasid() == null) {
            String errorMessage = "No sender canvas id found";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        User canvasSenderUser = userService.getUserByCanvasId(job.getSender_canvasid());

        if (canvasSenderUser == null) {
            String errorMessage = "Sender user not found in canvas";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        jobResult.setCanvasSenderUser(canvasSenderUser);

        edu.iu.uits.lms.canvasnotifier.model.User notifierSenderUser = userRepository.findByUsername(canvasSenderUser.getLoginId());

        if (notifierSenderUser == null) {
            String errorMessage = "Send user not found in notifier database";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        jobResult.setSenderDisplayName(notifierSenderUser.getDisplayName());

        if (! notifierSenderUser.isAuthorizedSender()) {
            String errorMessage = "Sender user is not an authorized sending user";

            jobResult.addErrorMessage(errorMessage);
            log.error("jobId: " + jobId + " - " + errorMessage);
            return;
        }

        jobResult.setJob(job);
    }

    public List<String> getRecipientsListForCanvasData(@NonNull List<String[]> csvContent) {
        List<String> usernameList = new ArrayList<>();

        for (String[] line: csvContent) {
            String username = line[0];

            if (! username.equals("username") && ! username.isEmpty()) {
                usernameList.add(username);
            }
        }

        return usernameList;
    }


    /**
     * Emails in two cases:
     * 1) The job isn't finished, but a terminating case occurs.  This status will be date_finished
     *    is null but there are error messages
     * 2) The job is finished (w/ or w/o errors)
     * @param jobResult
     * @return true is the calling handler should stop processing, False if it may continue (no email was sent)
     */
    private boolean emailIfReady(@NonNull JobResult jobResult) {
        // this is part of the validation job process. One would expect this to normally
        // return false here as no errors have been processed so far in validating the job
        // before job processing
        if (! JobStatus.FINISHED.equals(jobResult.getJob().getStatus()) && jobResult.getErrorMessages().size() == 0) {
            return false;
        }

        StringBuilder subjectTitleStringBuilder = new StringBuilder();
        subjectTitleStringBuilder.append("Canvas Notifier - ");

        StringBuilder finalSubjectStringBuilder = new StringBuilder();
        finalSubjectStringBuilder.append(emailService.getStandardHeader());
        finalSubjectStringBuilder.append(" - ");
        finalSubjectStringBuilder.append(subjectTitleStringBuilder);
        finalSubjectStringBuilder.append(" - ");
        finalSubjectStringBuilder.append("job #");
        finalSubjectStringBuilder.append(jobResult.getJobId());
        finalSubjectStringBuilder.append("\r\n");

        StringBuilder errorStringBuilder = new StringBuilder();

        for (String errorMessage : jobResult.getErrorMessages()) {
            log.error(errorMessage);
            errorStringBuilder.append(errorMessage);
            errorStringBuilder.append("\r\n");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(DateFormatUtil.JSON_DATE_FORMAT);

        errorStringBuilder.append("-------------------------\r\n");
        errorStringBuilder.append("JobId                : ");
        errorStringBuilder.append(jobResult.getJob().getId());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Initiator's username : ");
        errorStringBuilder.append(jobResult.getJob().getInitited_by_username());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Sender's display name: ");
        errorStringBuilder.append(jobResult.getSenderDisplayName());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Sender's username    : ");
        errorStringBuilder.append(jobResult.getCanvasSenderUser().getLoginId());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Message's subject    : ");
        errorStringBuilder.append(jobResult.getJob().getSubject());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Message's text       : ");
        errorStringBuilder.append(jobResult.getJob().getBody());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("-------------------------\r\n");
        errorStringBuilder.append("Job status           : ");
        errorStringBuilder.append(jobResult.getJob().getStatus());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Job start            : ");
        errorStringBuilder.append(dateFormat.format(jobResult.getJob().getCreatedOn()));
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Job end              : ");
        errorStringBuilder.append(dateFormat.format(jobResult.getJob().getModifiedOn()));
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Job duration         : ");
        errorStringBuilder.append(jobResult.getDurationOfJobString());
        errorStringBuilder.append("\r\n");

        errorStringBuilder.append("-------------------------\r\n");
        errorStringBuilder.append("Success count        : ");
        errorStringBuilder.append(jobResult.getProcessCounts().getSuccessCount());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Failure count        : ");
        errorStringBuilder.append(jobResult.getProcessCounts().getFailureCount());
        errorStringBuilder.append("\r\n");
        errorStringBuilder.append("Total count          : ");
        errorStringBuilder.append(jobResult.getProcessCounts().getTotalCount());
        errorStringBuilder.append("\r\n");

        String[] emailAddresses = ArrayUtils.add(toolConfig.getBatchNotificationEmail(), jobResult.getCanvasInitiatingUser().getEmail());
        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setRecipients(emailAddresses);
        emailDetails.setSubject(finalSubjectStringBuilder.toString());
        emailDetails.setBody(errorStringBuilder.toString());

        try {
            emailService.sendEmail(emailDetails);
        } catch (Exception e) {
            log.error("uh oh", e);
            return false;
        }

        log.debug("----- EMAIL: ");
        for (String emailAddress : emailAddresses) {
            log.debug("TO: " + emailAddress);
        }

        log.debug(finalSubjectStringBuilder.toString());
        log.debug(errorStringBuilder.toString());

        // send conversation
        ConversationCreateWrapper conversationCreateWrapper = new ConversationCreateWrapper();
        conversationCreateWrapper.setRecipients(new String[] {jobResult.getCanvasInitiatingUser().getId()});
        conversationCreateWrapper.setSubject(jobResult.getJob().getSubject());
        conversationCreateWrapper.setBody(errorStringBuilder.toString());
        conversationCreateWrapper.setContextCode(jobResult.getJob().getSubject());
        conversationCreateWrapper.setGroupConversation(false);

        Conversation resultsConversation = conversationService.postConversation(conversationCreateWrapper, jobResult.getCanvasSenderUser().getId(), false);

        if (resultsConversation != null) {
            log.debug("Initiator's conversation sent");
        } else {
            log.debug("Initiator's conversation NOT sent");
        }

        return true;
    }

    private String getCanvasUserIdFromUsername(String username, Map<String, String> usernameToCanvasidMap) {
        String canvasUserId = null;

        if (username == null || username.isEmpty()) {
            return canvasUserId;
        }

        canvasUserId = usernameToCanvasidMap.get(username);

        if (canvasUserId == null) {
            log.debug(username + " cannot find in canvas data. Trying instructure canvas lookup...");

            User user = userService.getUserBySisLoginId(username);

            if (user != null) {
                log.debug(username + " found in instructure canvas lookup");
                canvasUserId = user.getId();
            }

        }

        return canvasUserId;
    }

    /**
     * This is used to bypass the hibernate cache.  In case a REST endpoint or db update
     * actually wants this job to abort. Hibernate cache wouldn't see this change so
     * we use raw jdbc to bypass it
     * @param jobId
     * @return whether job in the database is set to abort
     */
    private boolean isAbortJob(@NonNull Long jobId) throws SQLException {
        String sql = "select status from lms.canvasnotifier_job where id = ?";
        Connection conn = dataSource.getConnection();
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        JobStatus status = null;

        try {
            preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setLong(1, jobId);
            rs = preparedStatement.executeQuery();

            if (rs.next()) {
                status =  JobStatus.valueOf(rs.getString("status"));
            }
        } catch (SQLException e) {
            log.error("uh oh", e);
        } finally {
            // close every thing
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException sqle) {
                log.error("Error closing resultset ", sqle);
            }

            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException sqle) {
                log.error("Error closing statement ", sqle);
            }

            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException sqle) {
                log.error("Error closing connection ", sqle);
            }
        }

        return JobStatus.ABORTED.equals(status);
    }

    /**
     * writes job to the database after bypassing hibernate cache and seeing what the current
     * abort job status is
     * @param jobResult
     */
    private void saveJob(@NonNull JobResult jobResult) {
        Job job = jobResult.getJob();

        if (job == null) {
            return;
        }

        boolean isAbortJob = false;
        try {
            isAbortJob = isAbortJob(job.getId());
        } catch (SQLException e) {
            log.error("uh oh", e);
        }

        if (isAbortJob) {
            job.abortJob();
        }

        jobResult.setJob(jobRepository.save(job));
    }

    /**
     *  This method checks the database for Recipient records and sets processedCounts in the jobResult
     *  correctly accordingly. This is for restarts and won't do anything (as no records will be found)
     *  for a new job
     * @param jobResult
     */
    private void computeCountsFromDatabase(@NonNull JobResult jobResult) {
        List<Recipient> processedRecipients = recipientRepository.findByJob(jobResult.getJob().getId());

        for (Recipient recipient : processedRecipients) {
            if (recipient.isSuccessfully_processed()) {
                jobResult.getProcessCounts().incrementSuccessCount();
            } else {
                jobResult.getProcessCounts().incrementFailureCount();

                if (recipient.getError() != null && recipient.getError().trim().length() > 0) {
                    jobResult.addErrorMessage(recipient.getError());
                }
            }
        }
    }
}
