package edu.iu.uits.lms.canvasnotifier.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessageSender;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.rest.JobRestController;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class JobRestControllerTest {
    @Autowired
    @InjectMocks
    private JobRestController jobRestController;

    @Autowired
    @Mock
    private JobRepository jobRepository;

    private MockMvc mockMvc;

    @Mock
    private CanvasNotifierMessageSender canvasNotifierMessageSender;

    @Mock
    private View view;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(jobRestController)
                .setSingleView(view)
                .build();
    }

    @Test
    public void testGetJob() throws Exception {
        Job job1 = new Job();
        job1.setId(1L);
        job1.setSubject("subject1");

//        Mockito.when(jobRepository.findOne(job1.getId())).thenReturn(job1);
        Mockito.when(jobRepository.findById(job1.getId())).thenReturn(java.util.Optional.of(job1));

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/rest/job/1")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult mvcResult = mockMvcAction.andReturn();

        String resultJson = mvcResult.getResponse().getContentAsString();

        Job resultJob = new Gson().fromJson(resultJson, Job.class);

        Assertions.assertNotNull(resultJson);
        Assertions.assertNotNull(resultJob);

        Assertions.assertEquals(job1.getId(), resultJob.getId());
        Assertions.assertEquals(job1.getSubject(), resultJob.getSubject());
    }

    @Test
    public void testGetAllJobs() throws Exception {
        List<Job> allJobsList = new ArrayList<>();

        Job job1 = new Job();
        job1.setId(1L);
        job1.setSubject("subject1");
        allJobsList.add(job1);

        Job job2 = new Job();
        job2.setId(2L);
        job2.setSubject("subject2");
        allJobsList.add(job2);

        Mockito.when(jobRepository.findAll()).thenReturn(allJobsList);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/rest/job/all")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult mvcResult = mockMvcAction.andReturn();

        String resultJson = mvcResult.getResponse().getContentAsString();

        Type customType = new TypeToken<ArrayList<Job>>(){}.getType();
        List<Job> resultJobsList = new Gson().fromJson(resultJson, customType);

        Assertions.assertNotNull(resultJson);
        Assertions.assertNotNull(resultJobsList);

        Assertions.assertEquals(2, resultJobsList.size());

        Assertions.assertEquals(job1.getId(), resultJobsList.get(0).getId());
        Assertions.assertEquals(job1.getSubject(), resultJobsList.get(0).getSubject());

        Assertions.assertEquals(job2.getId(), resultJobsList.get(1).getId());
        Assertions.assertEquals(job2.getSubject(), resultJobsList.get(1).getSubject());
    }

    @Test
    public void testAbortJob() throws Exception {
        Job job1 = new Job();
        job1.setId(1L);
        job1.setStatus(JobStatus.ABORTED);


        Job job2 = new Job();
        job2.setId(2L);
        job2.setModifiedOn(new Date());
        job2.setStatus(JobStatus.FINISHED);

        Job job3 = new Job();
        job3.setId(3L);
        job3.setStatus(JobStatus.STARTED);

        Assertions.assertFalse(JobStatus.ABORTED.equals(job3.getStatus()));

//        Mockito.when(jobRepository.findOne(job1.getId())).thenReturn(job1);
//        Mockito.when(jobRepository.findOne(job2.getId())).thenReturn(job2);
//        Mockito.when(jobRepository.findOne(job3.getId())).thenReturn(job3);
        Mockito.when(jobRepository.findById(job1.getId())).thenReturn(java.util.Optional.of(job1));
        Mockito.when(jobRepository.findById(job2.getId())).thenReturn(java.util.Optional.of(job2));
        Mockito.when(jobRepository.findById(job3.getId())).thenReturn(java.util.Optional.of(job3));

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/1/abort")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult mvcResult = mockMvcAction.andReturn();

        String resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job is already aborted", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/2/abort")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Cannot abort a finished job", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/3/abort")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job ABORTED", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/4/abort")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job not found", resultJson);
    }

    @Test
    public void testRestartJob() throws Exception {
        Job job1 = new Job();
        job1.setId(1L);

        Assertions.assertFalse(JobStatus.ABORTED.equals(job1.getStatus()));

        Job job2 = new Job();
        job2.setId(2L);
        job2.setStatus(JobStatus.ABORTED);

//        Mockito.when(jobRepository.findOne(job1.getId())).thenReturn(job1);
//        Mockito.when(jobRepository.findOne(job2.getId())).thenReturn(job2);
        Mockito.when(jobRepository.findById(job1.getId())).thenReturn(java.util.Optional.of(job1));
        Mockito.when(jobRepository.findById(job2.getId())).thenReturn(java.util.Optional.of(job2));

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/1/restart")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult mvcResult = mockMvcAction.andReturn();

        String resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job #1 is not in an aborted state", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/2/restart")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job #2 restarted", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/3/restart")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job not found", resultJson);
    }

}
