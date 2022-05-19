package edu.iu.uits.lms.canvasnotifier.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.Recipient;
import edu.iu.uits.lms.canvasnotifier.repository.RecipientRepository;
import edu.iu.uits.lms.canvasnotifier.rest.RecipientRestController;
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
import java.util.List;

@Slf4j
public class RecipientRestControllerTest {
    @Autowired
    @InjectMocks
    private RecipientRestController recipientRestController;

    @Autowired
    @Mock
    private RecipientRepository recipientRepository;

    private MockMvc mockMvc;

    @Mock
    private View view;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(recipientRestController)
                .setSingleView(view)
                .build();
    }

    @Test
    public void testGetRecipient() throws Exception {
        Recipient recipient1 = new Recipient();
        recipient1.setId(1L);
        recipient1.setUsername("user1");

//        Mockito.when(recipientRepository.findOne(recipient1.getId())).thenReturn(recipient1);
        Mockito.when(recipientRepository.findById(recipient1.getId())).thenReturn(java.util.Optional.of(recipient1));

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/rest/recipient/1")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult mvcResult = mockMvcAction.andReturn();

        String resultJson = mvcResult.getResponse().getContentAsString();

        Recipient resultRecipient = new Gson().fromJson(resultJson, Recipient.class);

        Assertions.assertNotNull(resultJson);
        Assertions.assertNotNull(resultRecipient);

        Assertions.assertEquals(recipient1.getId(), resultRecipient.getId());
        Assertions.assertEquals(recipient1.getUsername(), resultRecipient.getUsername());
    }

    @Test
    public void testGetAllRecipientsForUser() throws Exception {
        List<Recipient> allRecipientsList = new ArrayList<>();

        Job job = new Job();
        job.setId(999L);

        Recipient recipient1 = new Recipient();
        recipient1.setId(1L);
        recipient1.setJob(job);
        recipient1.setUsername("user1");
        allRecipientsList.add(recipient1);

        Recipient recipient2 = new Recipient();
        recipient2.setId(2L);
        recipient1.setJob(job);
        recipient2.setUsername("user2");
        allRecipientsList.add(recipient2);

        Mockito.when(recipientRepository.findByJob(job.getId())).thenReturn(allRecipientsList);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/rest/recipient/job/999")
                .contentType(MediaType.APPLICATION_JSON_UTF8));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult mvcResult = mockMvcAction.andReturn();

        String resultJson = mvcResult.getResponse().getContentAsString();

        Type customType = new TypeToken<ArrayList<Recipient>>(){}.getType();
        List<Recipient> resultRecipientList = new Gson().fromJson(resultJson, customType);

        Assertions.assertNotNull(resultJson);
        Assertions.assertNotNull(resultRecipientList);

        Assertions.assertEquals(2, resultRecipientList.size());

        Assertions.assertEquals(recipient1.getId(), resultRecipientList.get(0).getId());
        Assertions.assertEquals(recipient1.getUsername(), resultRecipientList.get(0).getUsername());

        Assertions.assertEquals(recipient2.getId(), resultRecipientList.get(1).getId());
        Assertions.assertEquals(recipient2.getUsername(), resultRecipientList.get(1).getUsername());
    }

}
