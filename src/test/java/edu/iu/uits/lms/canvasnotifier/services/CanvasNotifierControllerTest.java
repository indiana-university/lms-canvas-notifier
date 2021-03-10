package edu.iu.uits.lms.canvasnotifier.services;

import edu.iu.uits.lms.canvasnotifier.controller.CanvasNotifierController;
import edu.iu.uits.lms.canvasnotifier.model.User;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.repository.UserRepository;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
//import edu.iu.uits.lms.services.jms.JmsService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

@Slf4j
public class CanvasNotifierControllerTest {

    @Autowired
    @InjectMocks
    private CanvasNotifierController canvasNotifierController;

    @Autowired
    @Mock
    private JobRepository jobRepository;

//    @Autowired
//    @Mock
//    private JmsService jmsService;

    @Autowired
    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @Mock
    private View view;

    private static final String ID = "12345";

    private enum Method { GET, POST }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        //setup lti token
        List authoritiesList = Arrays.asList(new SimpleGrantedAuthority("Instructor"),
                new SimpleGrantedAuthority("ROLE_LTI_INSTRUCTOR"));

        Map<String, Object> dataMap = new HashMap<>();


        final LtiAuthenticationToken ltiAuthenticationToken = new LtiAuthenticationToken("user1",
                ID,
                "test.uits.iu.edu",
                authoritiesList,
                dataMap,
                "canvasnotifier");

        SecurityContextHolder.getContext().setAuthentication(ltiAuthenticationToken);

        Mockito.reset(userRepository);

        mockMvc = MockMvcBuilders
                .standaloneSetup(canvasNotifierController)
                .setSingleView(view)
                .build();
    }

    private void authorizedUserAccess(@NonNull CanvasNotifierControllerTest.Method method, @NonNull String page, @NonNull String viewName) throws Exception {
        ResultActions mockMvcAction = null;

        switch (method) {
            case GET:
                mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/app/" + page, ID));
                break;
            case POST:
                mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.post("/app/" + page, ID));
                break;
            default:
                throw new RuntimeException();
        }

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());
        mockMvcAction.andExpect(MockMvcResultMatchers.view().name(viewName));
    }

    @Test
    public void testAllowAuthorizedUserAccessMain() throws Exception {
        User user = new User();
        user.setAuthorizedUser(true);

        Mockito.when(userRepository.findByUsername("user1")).thenReturn(user);

        authorizedUserAccess(Method.GET, "main", "main");
    }

    @Test
    public void testMainSenderList() throws Exception {
        final String username1 = "user1";
        final String username2 = "user2";

        final String displayName1 = "User 1";
        final String displayName2 = "User 2";

        final String canvasId1="12345";
        final String canvasId2="67890";

        User authorizedButNotSenderUser = new User();
        authorizedButNotSenderUser.setAuthorizedUser(true);
        authorizedButNotSenderUser.setAuthorizedSender(false);
        authorizedButNotSenderUser.setDisplayName(displayName1);
        authorizedButNotSenderUser.setUsername(username1);
        authorizedButNotSenderUser.setCanvasUserId(canvasId1);

        User notAuthorizedButSenderUser = new User();
        notAuthorizedButSenderUser.setId(1L);
        notAuthorizedButSenderUser.setAuthorizedUser(false);
        notAuthorizedButSenderUser.setAuthorizedSender(true);
        notAuthorizedButSenderUser.setDisplayName(displayName2);
        notAuthorizedButSenderUser.setUsername(username2);
        notAuthorizedButSenderUser.setCanvasUserId(canvasId2);


        List<User> userRepositoryResultList = new ArrayList<>();
        userRepositoryResultList.add(notAuthorizedButSenderUser);

        Mockito.when(userRepository.findByUsername("user1")).thenReturn(authorizedButNotSenderUser);
        Mockito.when(userRepository.findAllAuthorizedSenders()).thenReturn(userRepositoryResultList);

        ResultActions mockMvcAction = mockMvc.perform(MockMvcRequestBuilders.get("/app/main", ID));

        mockMvcAction.andExpect(MockMvcResultMatchers.status().isOk());

        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists("canvasNotifierFormModel"));

        mockMvcAction.andExpect(MockMvcResultMatchers.model().attribute("canvasNotifierFormModel",
                hasProperty("userList", hasSize(2))));

        // "choose a username..." dummy option
        mockMvcAction.andExpect(MockMvcResultMatchers.model().attribute("canvasNotifierFormModel",
                hasProperty("userList",
                        hasItem(hasProperty("id",
                                equalTo(-1L))))));

        mockMvcAction.andExpect(MockMvcResultMatchers.model().attribute("canvasNotifierFormModel",
                hasProperty("userList",
                        hasItem(hasProperty("username",
                                not(equalTo(authorizedButNotSenderUser.getUsername())))))));

        mockMvcAction.andExpect(MockMvcResultMatchers.model().attribute("canvasNotifierFormModel",
                hasProperty("userList",
                        hasItem(hasProperty("username", equalTo(notAuthorizedButSenderUser.getUsername()))))));

    }
}
