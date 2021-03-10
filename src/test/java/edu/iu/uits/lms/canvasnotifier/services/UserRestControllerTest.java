package edu.iu.uits.lms.canvasnotifier.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iu.uits.lms.canvasnotifier.repository.UserRepository;
import edu.iu.uits.lms.canvasnotifier.rest.RestUser;
import edu.iu.uits.lms.canvasnotifier.rest.UserRestController;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import java.io.IOException;

@Slf4j
public class UserRestControllerTest {
    @Autowired
    @InjectMocks
    private UserRestController userRestController;

    @Autowired
    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @Mock
    private View view;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(userRestController)
                .setSingleView(view)
                .build();
    }

    @Test
    public void testCreateEndpoint() throws Exception {
        RestUser restUser = new RestUser();
        restUser.setUsername("foobar");
        restUser.setDisplayName("Foo Bar");
        restUser.setCanvasUserId("12345");
        restUser.setAuthorizedSender(Boolean.TRUE);
        restUser.setAuthorizedUser(Boolean.TRUE);

        String content = convertObjectToJsonString(restUser);
        log.debug(content);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/rest/user")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(content));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
    }

    /**
     * Serialize an object into a byte array
     * @param object Object to serialize
     * @return Serialized results
     * @throws IOException If error with the ObjectMapper
     */
    private static String convertObjectToJsonString(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }
}
