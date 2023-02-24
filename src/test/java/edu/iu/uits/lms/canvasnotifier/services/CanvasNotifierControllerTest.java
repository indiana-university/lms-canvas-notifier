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

import edu.iu.uits.lms.canvasnotifier.controller.CanvasNotifierController;
import edu.iu.uits.lms.canvasnotifier.model.User;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.repository.UserRepository;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    @Autowired
    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @Mock
    private View view;

    private static final String ID = "12345";

    private enum Method { GET, POST }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        //setup lti token
        List authoritiesList = Arrays.asList(new SimpleGrantedAuthority("Instructor"),
                new SimpleGrantedAuthority("ROLE_LTI_INSTRUCTOR"));

//        final LtiAuthenticationToken ltiAuthenticationToken = new LtiAuthenticationToken("user1",
//                ID,
//                "test.uits.iu.edu",
//                authoritiesList,
//                "canvasnotifier");
        OidcAuthenticationToken token = TestUtils.buildToken("userId", ID, LTIConstants.INSTRUCTOR_AUTHORITY);

        SecurityContextHolder.getContext().setAuthentication(token);

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
