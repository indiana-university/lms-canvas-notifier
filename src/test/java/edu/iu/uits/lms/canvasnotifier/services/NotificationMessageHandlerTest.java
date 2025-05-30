package edu.iu.uits.lms.canvasnotifier.services;

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

import edu.iu.uits.lms.canvas.model.User;
import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.ConversationService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.canvasnotifier.Constants;
import edu.iu.uits.lms.canvasnotifier.config.ApplicationConfig;
import edu.iu.uits.lms.canvasnotifier.config.ToolConfig;
import edu.iu.uits.lms.canvasnotifier.handler.JobResult;
import edu.iu.uits.lms.canvasnotifier.handler.NotificationMessageHandler;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.repository.RecipientRepository;
import edu.iu.uits.lms.canvasnotifier.service.CanvasNotifierService;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.iuonly.model.acl.AuthorizedUser;
import edu.iu.uits.lms.iuonly.model.acl.ToolPermission;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.iuonly.services.CanvasDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(classes = {NotificationMessageHandler.class, CanvasNotifierService.class, ApplicationConfig.class},
        properties = {"oauth.tokenprovider.url=http://foo", "logging.level.org.springframework.security=DEBUG"})
@Slf4j
public class NotificationMessageHandlerTest {
    @Autowired
    private NotificationMessageHandler notificationMessageHandler;

    @Autowired
    private CanvasNotifierService canvasNotifierService;

    @MockitoBean
    private AccountService accountsApi;

    @MockitoBean
    private CanvasService canvasApi;

    @MockitoBean
    private CanvasDataServiceImpl canvasDataApi;

    @MockitoBean
    private ConversationService conversationsApi;

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private EmailService emailApi;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private RecipientRepository recipientRepository;

    @MockitoBean
    private ToolConfig toolConfig;

    @MockitoBean
    private AuthorizedUserService authorizedUserService;

    @MockitoBean
    private UserService usersApi;

    private final String[] recipients = new String[]{"senduser1", "senduser2", "senduser3", "senduser4"};

    private String jsonCsvString;

    @BeforeEach
    public void setup() {
        jsonCsvString = "[[\"username\",\" team\",\" superhero\"]," +
                  "[\"" + recipients[0] + "\",\" Fuel\",\" spiderman\"]," +
                  "[\"" + recipients[1] + "\",\" Bills\",\" Wonder Woman\"]," +
                  "[\"" + recipients[2] + "\",\" Express\",\" batman\"]," +
                  "[\"" + recipients[3] + "\",\" crap team\",\" me\"],[\"\"]]";

        Mockito.reset(jobRepository, authorizedUserService, usersApi);
    }

    @Test
    public void testValidateNullJob() {
        JobResult jobResult = new JobResult();

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("JobId can't be empty"::equals));
    }

    @Test
    public void testValidateNotFoundJob() {
        JobResult jobResult = new JobResult();
        jobResult.setJobId(1L);

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("Can't find jobId"::equals));
    }

    @Test
    public void testValidateFoundJobButNoCsv() {
        Long jobId = 1L;

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("No csv data found"::equals));
    }

    @Test
    public void testValidateFoundJobButNoSubject() {
        Long jobId = 1L;

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("No subject found"::equals));
    }

    @Test
    public void testValidateFoundJobButNoBody() {
        Long jobId = 1L;

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("No body found"::equals));
    }

    @Test
    public void testValidateFoundJobButNoInitiatedByUser() {
        Long jobId = 1L;

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("No initiated by user found"::equals));
    }

    @Test
    public void testValidateFoundJobButCannotLookupInCanvasInitiatedByUser() {
        Long jobId = 1L;
        String initiatedByUsername = "user1";

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");
        foundJob.setInitited_by_username(initiatedByUsername);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("Initiated by user not found in canvas"::equals));
    }

    @Test
    public void testValidateFoundJobButInitiatedByUserNotFoundInNotifierDatabase() {
        Long jobId = 1L;
        String initiatedByUsername = "user1";

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");
        foundJob.setInitited_by_username(initiatedByUsername);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        User canvasUser = new User();
        canvasUser.setLoginId(initiatedByUsername);

        Mockito.when(usersApi.getUserBySisLoginId(initiatedByUsername)).thenReturn(canvasUser);

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("Initiated by user is not an authorized user"::equals));
    }

    @Test
    public void testValidateFoundJobButInitiatedByUserNotAuthorized() {
        Long jobId = 1L;
        String initiatedByUsername = "user1";

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");
        foundJob.setInitited_by_username(initiatedByUsername);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        User canvasUser = new User();
        canvasUser.setLoginId(initiatedByUsername);

        Mockito.when(usersApi.getUserBySisLoginId(initiatedByUsername)).thenReturn(canvasUser);

        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission(initiatedByUsername, Constants.AUTH_USER_TOOL_PERMISSION)).thenReturn(null);

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("Initiated by user is not an authorized user"::equals));
    }

    @Test
    public void testValidateFoundJobButSenderCanvasIdNull() {
        Long jobId = 1L;
        String initiatedByUsername = "user1";

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");
        foundJob.setInitited_by_username(initiatedByUsername);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        User canvasUser = new User();
        canvasUser.setLoginId(initiatedByUsername);

        Mockito.when(usersApi.getUserBySisLoginId(initiatedByUsername)).thenReturn(canvasUser);

        AuthorizedUser notifierUser = new AuthorizedUser();
        notifierUser.setActive(true);
        Map<String, ToolPermission> toolPermissionMap = new HashMap<>();
        toolPermissionMap.put(Constants.AUTH_USER_TOOL_PERMISSION, new ToolPermission());
        notifierUser.setToolPermissions(toolPermissionMap);

        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission(initiatedByUsername, Constants.AUTH_USER_TOOL_PERMISSION)).thenReturn(notifierUser);

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("No sender canvas id found"::equals));
    }

    @Test
    public void testValidateFoundJobButSenderCanvasIdNotFoundInCanvas() {
        Long jobId = 1L;
        String initiatedByUsername = "user1";
        String sendCanvasId = "77777";

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");
        foundJob.setInitited_by_username(initiatedByUsername);
        foundJob.setSender_canvasid(sendCanvasId);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        User initiatedByCanvasUser = new User();
        initiatedByCanvasUser.setLoginId(initiatedByUsername);

        Mockito.when(usersApi.getUserBySisLoginId(initiatedByUsername)).thenReturn(initiatedByCanvasUser);

        AuthorizedUser notifierUser = new AuthorizedUser();
        notifierUser.setActive(true);
        Map<String, ToolPermission> toolPermissionMap = new HashMap<>();
        toolPermissionMap.put(Constants.AUTH_USER_TOOL_PERMISSION, new ToolPermission());
        notifierUser.setToolPermissions(toolPermissionMap);

        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission(initiatedByUsername, Constants.AUTH_USER_TOOL_PERMISSION)).thenReturn(notifierUser);

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("Sender user not found in canvas"::equals));
    }

    @Test
    public void testValidateFoundJobButSenderUserNotFoundInNotifierDatabase() {
        Long jobId = 1L;
        String initiatedByUsername = "user1";
        String sendCanvasId = "77777";
        String sendUsername = "user2";

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");
        foundJob.setInitited_by_username(initiatedByUsername);
        foundJob.setSender_canvasid(sendCanvasId);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        User initiatedByCanvasUser = new User();
        initiatedByCanvasUser.setLoginId(initiatedByUsername);

        User senderUser = new User();
        senderUser.setId(sendCanvasId);
        senderUser.setLoginId(sendUsername);

        Mockito.when(usersApi.getUserBySisLoginId(initiatedByUsername)).thenReturn(initiatedByCanvasUser);
        Mockito.when(usersApi.getUserByCanvasId(sendCanvasId)).thenReturn(senderUser);

        AuthorizedUser notifierUser = new AuthorizedUser();
        notifierUser.setActive(true);
        Map<String, ToolPermission> toolPermissionMap = new HashMap<>();
        toolPermissionMap.put(Constants.AUTH_USER_TOOL_PERMISSION, new ToolPermission());
        notifierUser.setToolPermissions(toolPermissionMap);

        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission(initiatedByUsername, Constants.AUTH_USER_TOOL_PERMISSION)).thenReturn(notifierUser);

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assertions.assertNotNull(jobResult.getCanvasSenderUser());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("Sender user is not an authorized sending user"::equals));
    }

    @Test
    public void testValidateFoundJobButSenderUserNotASender() {
        Long jobId = 1L;
        String initiatedByUsername = "user1";
        String sendCanvasId = "77777";
        String sendUsername = "user2";

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");
        foundJob.setInitited_by_username(initiatedByUsername);
        foundJob.setSender_canvasid(sendCanvasId);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        User initiatedByCanvasUser = new User();
        initiatedByCanvasUser.setLoginId(initiatedByUsername);

        User senderUser = new User();
        senderUser.setId(sendCanvasId);
        senderUser.setLoginId(sendUsername);

        Mockito.when(usersApi.getUserBySisLoginId(initiatedByUsername)).thenReturn(initiatedByCanvasUser);
        Mockito.when(usersApi.getUserByCanvasId(sendCanvasId)).thenReturn(senderUser);

        AuthorizedUser notifierInitiatedByUser = new AuthorizedUser();
        notifierInitiatedByUser.setActive(true);
        Map<String, ToolPermission> toolPermissionMap = new HashMap<>();
        toolPermissionMap.put(Constants.AUTH_USER_TOOL_PERMISSION, new ToolPermission());
        notifierInitiatedByUser.setToolPermissions(toolPermissionMap);

        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission(initiatedByUsername, Constants.AUTH_USER_TOOL_PERMISSION)).thenReturn(notifierInitiatedByUser);
        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission(sendUsername, Constants.AUTH_SENDER_TOOL_PERMISSION)).thenReturn(null);

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNull(jobResult.getJob());
        Assertions.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assertions.assertNotNull(jobResult.getCanvasSenderUser());
        Assertions.assertTrue(jobResult.getErrorMessages().stream().anyMatch("Sender user is not an authorized sending user"::equals));
    }

    @Test
    public void testValidateValidJob() {
        Long jobId = 1L;
        String initiatedByUsername = "user1";
        String sendCanvasId = "77777";
        String sendUsername = "user2";

        JobResult jobResult = new JobResult();
        jobResult.setJobId(jobId);

        Job foundJob = new Job();
        foundJob.setId(jobId);
        foundJob.setJson_csv(jsonCsvString);
        foundJob.setSubject("subject");
        foundJob.setBody("body");
        foundJob.setInitited_by_username(initiatedByUsername);
        foundJob.setSender_canvasid(sendCanvasId);

//        Mockito.when(jobRepository.findOne(jobId)).thenReturn(foundJob);
        Mockito.when(jobRepository.findById(jobId)).thenReturn(java.util.Optional.of(foundJob));

        User initiatedByCanvasUser = new User();
        initiatedByCanvasUser.setLoginId(initiatedByUsername);

        User senderUser = new User();
        senderUser.setId(sendCanvasId);
        senderUser.setLoginId(sendUsername);

        Mockito.when(usersApi.getUserBySisLoginId(initiatedByUsername)).thenReturn(initiatedByCanvasUser);
        Mockito.when(usersApi.getUserByCanvasId(sendCanvasId)).thenReturn(senderUser);

        AuthorizedUser notifierInitiatedByUser = new AuthorizedUser();
        notifierInitiatedByUser.setActive(true);
        Map<String, ToolPermission> toolPermissionMap = new HashMap<>();
        toolPermissionMap.put(Constants.AUTH_USER_TOOL_PERMISSION, new ToolPermission());
        notifierInitiatedByUser.setToolPermissions(toolPermissionMap);

        AuthorizedUser notifierSenderUser = new AuthorizedUser();
        notifierSenderUser.setActive(true);
        Map<String, ToolPermission> toolPermissionMap2 = new HashMap<>();
        toolPermissionMap2.put(Constants.AUTH_SENDER_TOOL_PERMISSION, new ToolPermission());
        notifierSenderUser.setToolPermissions(toolPermissionMap2);

        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission(initiatedByUsername, Constants.AUTH_USER_TOOL_PERMISSION)).thenReturn(notifierInitiatedByUser);
        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission(sendUsername, Constants.AUTH_SENDER_TOOL_PERMISSION)).thenReturn(notifierSenderUser);

        notificationMessageHandler.validateJob(jobResult);

        Assertions.assertNotNull(jobResult.getJob());
        Assertions.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assertions.assertNotNull(jobResult.getCanvasSenderUser());
        Assertions.assertEquals(0, jobResult.getErrorMessages().size());
    }

    @Test
    public void testGetRecipientsListforCanvasData() {
        List<String[]> csvContent = new ArrayList<>();

        String[] headerLine = new String[] {"username", "team", "superhero"};
        csvContent.add(headerLine);

        csvContent.add(new String[] {recipients[0], "team1", "superhero1"});
        csvContent.add(new String[] {recipients[1], "team2", "superhero2"});
        csvContent.add(new String[] {recipients[2], "team3", "superhero3"});
        csvContent.add(new String[] {recipients[3], "team4", "superhero4"});
        csvContent.add(new String[] {"", "", ""});

        List<String> recipientsListForCanvasData = notificationMessageHandler.getRecipientsListForCanvasData(csvContent);

        Assertions.assertNotEquals(csvContent.size(), recipientsListForCanvasData.size());

        // should not be header line nor the empty string line
        Assertions.assertEquals(csvContent.size() - 2, recipientsListForCanvasData.size());

        Assertions.assertEquals(recipients[0], recipientsListForCanvasData.get(0));
        Assertions.assertEquals(recipients[1], recipientsListForCanvasData.get(1));
        Assertions.assertEquals(recipients[2], recipientsListForCanvasData.get(2));
        Assertions.assertEquals(recipients[3], recipientsListForCanvasData.get(3));
    }
}
