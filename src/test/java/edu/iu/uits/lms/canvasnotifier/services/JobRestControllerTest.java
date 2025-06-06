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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessageSender;
import edu.iu.uits.lms.canvasnotifier.config.ApplicationConfig;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.rest.JobRestController;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = JobRestController.class, properties = {"oauth.tokenprovider.url=http://foo", "logging.level.org.springframework.security=DEBUG"})
@ContextConfiguration(classes = {ApplicationConfig.class, JobRestController.class})
@Slf4j
public class JobRestControllerTest {
    @Autowired
    private JobRestController jobRestController;

    @MockitoBean
    private JobRepository jobRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CanvasNotifierMessageSender canvasNotifierMessageSender;

    @MockitoBean
    private AuthorizedUserService authorizedUserService;

    @BeforeEach
    public void setup() {
        Map<String, Object> extraAttributes = new HashMap<>();

        JSONObject customMap = new JSONObject();
        customMap.put(LTIConstants.CUSTOM_CANVAS_USER_LOGIN_ID_KEY, "user1");

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
                extraAttributes, customMap);

        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    public void testGetJob() throws Exception {
        Job job1 = new Job();
        job1.setId(1L);
        job1.setSubject("subject1");

//        Mockito.when(jobRepository.findOne(job1.getId())).thenReturn(job1);
        Mockito.when(jobRepository.findById(job1.getId())).thenReturn(java.util.Optional.of(job1));

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/rest/job/1")
                .contentType(MediaType.APPLICATION_JSON));

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
                .contentType(MediaType.APPLICATION_JSON));

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

        Assertions.assertNotEquals(JobStatus.ABORTED, job3.getStatus());

//        Mockito.when(jobRepository.findOne(job1.getId())).thenReturn(job1);
//        Mockito.when(jobRepository.findOne(job2.getId())).thenReturn(job2);
//        Mockito.when(jobRepository.findOne(job3.getId())).thenReturn(job3);
        Mockito.when(jobRepository.findById(job1.getId())).thenReturn(java.util.Optional.of(job1));
        Mockito.when(jobRepository.findById(job2.getId())).thenReturn(java.util.Optional.of(job2));
        Mockito.when(jobRepository.findById(job3.getId())).thenReturn(java.util.Optional.of(job3));

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/1/abort").with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult mvcResult = mockMvcAction.andReturn();

        String resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job is already aborted", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/2/abort").with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Cannot abort a finished job", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/3/abort").with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job ABORTED", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/4/abort").with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job not found", resultJson);
    }

    @Test
    public void testRestartJob() throws Exception {
        Job job1 = new Job();
        job1.setId(1L);

        Assertions.assertNotEquals(JobStatus.ABORTED, job1.getStatus());

        Job job2 = new Job();
        job2.setId(2L);
        job2.setStatus(JobStatus.ABORTED);

//        Mockito.when(jobRepository.findOne(job1.getId())).thenReturn(job1);
//        Mockito.when(jobRepository.findOne(job2.getId())).thenReturn(job2);
        Mockito.when(jobRepository.findById(job1.getId())).thenReturn(java.util.Optional.of(job1));
        Mockito.when(jobRepository.findById(job2.getId())).thenReturn(java.util.Optional.of(job2));

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/1/restart").with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult mvcResult = mockMvcAction.andReturn();

        String resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job #1 is not in an aborted state", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/2/restart").with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job #2 restarted", resultJson);

        mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.put("/rest/job/3/restart").with(csrf())
                .contentType(MediaType.APPLICATION_JSON));

        mvcResult = mockMvcAction.andReturn();

        resultJson = mvcResult.getResponse().getContentAsString();

        Assertions.assertNotNull(resultJson);

        Assertions.assertEquals("Job not found", resultJson);
    }

}
