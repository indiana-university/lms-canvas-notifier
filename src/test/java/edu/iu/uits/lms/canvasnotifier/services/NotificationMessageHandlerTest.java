package edu.iu.uits.lms.canvasnotifier.services;

import canvas.client.generated.api.AccountsApi;
import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.ConversationsApi;
import canvas.client.generated.api.UsersApi;
import canvas.client.generated.model.User;
import edu.iu.uits.lms.canvasnotifier.config.ToolConfig;
import edu.iu.uits.lms.canvasnotifier.handler.JobResult;
import edu.iu.uits.lms.canvasnotifier.handler.NotificationMessageHandler;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.repository.RecipientRepository;
import edu.iu.uits.lms.canvasnotifier.repository.UserRepository;
import email.client.generated.api.EmailApi;
import iuonly.client.generated.api.CanvasDataApi;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NotificationMessageHandlerTest {
    @Autowired
    @InjectMocks
    private NotificationMessageHandler notificationMessageHandler;

    @Mock
    private AccountsApi accountsApi;

    @Mock
    private CanvasApi canvasApi;

    @Mock
    private CanvasDataApi canvasDataApi;

    @Mock
    private ConversationsApi conversationsApi;

    @Mock
    private DataSource dataSource;

    @Mock
    private EmailApi emailApi;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private RecipientRepository recipientRepository;

    @Mock
    private ToolConfig toolConfig;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsersApi usersApi;

    private final String[] recipients = new String[]{"senduser1", "senduser2", "senduser3", "senduser4"};

    private String jsonCsvString;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        jsonCsvString = "[[\"username\",\" team\",\" superhero\"]," +
                  "[\"" + recipients[0] + "\",\" Fuel\",\" spiderman\"]," +
                  "[\"" + recipients[1] + "\",\" Bills\",\" Wonder Woman\"]," +
                  "[\"" + recipients[2] + "\",\" Express\",\" batman\"]," +
                  "[\"" + recipients[3] + "\",\" crap team\",\" me\"],[\"\"]]";

        Mockito.reset(jobRepository, userRepository, usersApi);


    }

    @Test
    public void placeHolder() {

    }

    @Test
    public void testValidateNullJob() {
        JobResult jobResult = new JobResult();

        notificationMessageHandler.validateJob(jobResult);

        Assert.assertNull(jobResult.getJob());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "JobId can't be empty".equals(em)));
    }

    @Test
    public void testValidateNotFoundJob() {
        JobResult jobResult = new JobResult();
        jobResult.setJobId(1L);

        notificationMessageHandler.validateJob(jobResult);

        Assert.assertNull(jobResult.getJob());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "Can't find jobId".equals(em)));
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

        Assert.assertNull(jobResult.getJob());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "No csv data found".equals(em)));
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

        Assert.assertNull(jobResult.getJob());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "No subject found".equals(em)));
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

        Assert.assertNull(jobResult.getJob());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "No body found".equals(em)));
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

        Assert.assertNull(jobResult.getJob());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "No initiated by user found".equals(em)));
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

        Assert.assertNull(jobResult.getJob());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "Initiated by user not found in canvas".equals(em)));
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

        Assert.assertNull(jobResult.getJob());
        Assert.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "Initiated by user not found in notifier database".equals(em)));
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

        edu.iu.uits.lms.canvasnotifier.model.User notifierUser = new edu.iu.uits.lms.canvasnotifier.model.User();
        notifierUser.setAuthorizedUser(false);

        Mockito.when(userRepository.findByUsername(initiatedByUsername)).thenReturn(notifierUser);

        notificationMessageHandler.validateJob(jobResult);

        Assert.assertNull(jobResult.getJob());
        Assert.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "Initiated by user is not an authorized user".equals(em)));
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

        edu.iu.uits.lms.canvasnotifier.model.User notifierUser = new edu.iu.uits.lms.canvasnotifier.model.User();
        notifierUser.setAuthorizedUser(true);

        Mockito.when(userRepository.findByUsername(initiatedByUsername)).thenReturn(notifierUser);

        notificationMessageHandler.validateJob(jobResult);

        Assert.assertNull(jobResult.getJob());
        Assert.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "No sender canvas id found".equals(em)));
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

        edu.iu.uits.lms.canvasnotifier.model.User notifierUser = new edu.iu.uits.lms.canvasnotifier.model.User();
        notifierUser.setAuthorizedUser(true);

        Mockito.when(userRepository.findByUsername(initiatedByUsername)).thenReturn(notifierUser);

        notificationMessageHandler.validateJob(jobResult);

        Assert.assertNull(jobResult.getJob());
        Assert.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "Sender user not found in canvas".equals(em)));
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

        edu.iu.uits.lms.canvasnotifier.model.User notifierUser = new edu.iu.uits.lms.canvasnotifier.model.User();
        notifierUser.setAuthorizedUser(true);

        Mockito.when(userRepository.findByUsername(initiatedByUsername)).thenReturn(notifierUser);

        notificationMessageHandler.validateJob(jobResult);

        Assert.assertNull(jobResult.getJob());
        Assert.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assert.assertNotNull(jobResult.getCanvasSenderUser());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "Send user not found in notifier database".equals(em)));
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

        edu.iu.uits.lms.canvasnotifier.model.User notifierInitiatedByUser = new edu.iu.uits.lms.canvasnotifier.model.User();
        notifierInitiatedByUser.setAuthorizedUser(true);

        edu.iu.uits.lms.canvasnotifier.model.User notifierSenderUser = new edu.iu.uits.lms.canvasnotifier.model.User();
        notifierSenderUser.setAuthorizedSender(false);

        Mockito.when(userRepository.findByUsername(initiatedByUsername)).thenReturn(notifierInitiatedByUser);
        Mockito.when(userRepository.findByUsername(sendUsername)).thenReturn(notifierSenderUser);

        notificationMessageHandler.validateJob(jobResult);

        Assert.assertNull(jobResult.getJob());
        Assert.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assert.assertNotNull(jobResult.getCanvasSenderUser());
        Assert.assertTrue(jobResult.getErrorMessages().stream().anyMatch(em -> "Sender user is not an authorized sending user".equals(em)));
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

        edu.iu.uits.lms.canvasnotifier.model.User notifierInitiatedByUser = new edu.iu.uits.lms.canvasnotifier.model.User();
        notifierInitiatedByUser.setAuthorizedUser(true);

        edu.iu.uits.lms.canvasnotifier.model.User notifierSenderUser = new edu.iu.uits.lms.canvasnotifier.model.User();
        notifierSenderUser.setAuthorizedSender(true);

        Mockito.when(userRepository.findByUsername(initiatedByUsername)).thenReturn(notifierInitiatedByUser);
        Mockito.when(userRepository.findByUsername(sendUsername)).thenReturn(notifierSenderUser);

        notificationMessageHandler.validateJob(jobResult);

        Assert.assertNotNull(jobResult.getJob());
        Assert.assertNotNull(jobResult.getCanvasInitiatingUser());
        Assert.assertNotNull(jobResult.getCanvasSenderUser());
        Assert.assertEquals(0, jobResult.getErrorMessages().size());
    }

    @Test
    public void testGetRecipentsListforCanvasData() {
        List<String[]> csvContent = new ArrayList<>();

        String[] headerLine = new String[] {"username", "team", "superhero"};
        csvContent.add(headerLine);

        csvContent.add(new String[] {recipients[0], "team1", "superhero1"});
        csvContent.add(new String[] {recipients[1], "team2", "superhero2"});
        csvContent.add(new String[] {recipients[2], "team3", "superhero3"});
        csvContent.add(new String[] {recipients[3], "team4", "superhero4"});
        csvContent.add(new String[] {"", "", ""});

        List<String> recipentsListForCanvasData = notificationMessageHandler.getRecipentsListForCanvasData(csvContent);

        Assert.assertNotEquals(csvContent.size(), recipentsListForCanvasData.size());

        // should not be header line nor the empty string line
        Assert.assertEquals(csvContent.size() - 2, recipentsListForCanvasData.size());

        Assert.assertEquals(recipients[0], recipentsListForCanvasData.get(0));
        Assert.assertEquals(recipients[1], recipentsListForCanvasData.get(1));
        Assert.assertEquals(recipients[2], recipentsListForCanvasData.get(2));
        Assert.assertEquals(recipients[3], recipentsListForCanvasData.get(3));
    }
}