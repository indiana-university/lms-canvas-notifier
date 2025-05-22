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

import edu.iu.uits.lms.canvasnotifier.Constants;
import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessageSender;
import edu.iu.uits.lms.canvasnotifier.config.ApplicationConfig;
import edu.iu.uits.lms.canvasnotifier.controller.CanvasNotifierController;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.iuonly.model.acl.AuthorizedUser;
import edu.iu.uits.lms.iuonly.model.acl.ToolPermission;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

@WebMvcTest(controllers = CanvasNotifierController.class, properties = {"oauth.tokenprovider.url=http://foo", "logging.level.org.springframework.security=DEBUG"})
@ContextConfiguration(classes = {ApplicationConfig.class, CanvasNotifierController.class})
@Slf4j
public class CanvasNotifierControllerTest {

    @Autowired
    private CanvasNotifierController canvasNotifierController;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private AuthorizedUserService authorizedUserService;

    @MockitoBean
    private CanvasNotifierMessageSender canvasNotifierMessageSender;

    @MockitoBean(name = ServerInfo.BEAN_NAME)
    private ServerInfo serverInfo;

    @Autowired
    private MockMvc mockMvc;

    private static final String ID = "12345";

    private enum Method { GET, POST }

    @BeforeEach
    public void setup() {
        OidcAuthenticationToken token = TestUtils.buildToken("userId", ID, LTIConstants.INSTRUCTOR_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(token);

        Mockito.reset(authorizedUserService);
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
        AuthorizedUser user = new AuthorizedUser();
        user.setActive(true);

        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission("user1", Constants.AUTH_USER_TOOL_PERMISSION)).thenReturn(user);

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

        AuthorizedUser authorizedButNotSenderUser = new AuthorizedUser();
        authorizedButNotSenderUser.setActive(true);
        authorizedButNotSenderUser.setDisplayName(displayName1);
        authorizedButNotSenderUser.setUsername(username1);
        authorizedButNotSenderUser.setCanvasUserId(canvasId1);
        Map<String, ToolPermission> toolPermissionMap = new HashMap<>();
        toolPermissionMap.put(Constants.AUTH_USER_TOOL_PERMISSION, new ToolPermission());
        authorizedButNotSenderUser.setToolPermissions(toolPermissionMap);

        AuthorizedUser notAuthorizedButSenderUser = new AuthorizedUser();
        notAuthorizedButSenderUser.setId(1L);
        notAuthorizedButSenderUser.setDisplayName(displayName2);
        notAuthorizedButSenderUser.setUsername(username2);
        notAuthorizedButSenderUser.setCanvasUserId(canvasId2);
        Map<String, ToolPermission> toolPermissionMap2 = new HashMap<>();
        toolPermissionMap2.put(Constants.AUTH_SENDER_TOOL_PERMISSION, new ToolPermission());
        notAuthorizedButSenderUser.setToolPermissions(toolPermissionMap2);


        List<AuthorizedUser> userRepositoryResultList = new ArrayList<>();
        userRepositoryResultList.add(notAuthorizedButSenderUser);

        Mockito.when(authorizedUserService.findByActiveUsernameAndToolPermission("user1", Constants.AUTH_USER_TOOL_PERMISSION)).thenReturn(authorizedButNotSenderUser);
        Mockito.when(authorizedUserService.findActiveUsersByPermission(Constants.AUTH_SENDER_TOOL_PERMISSION)).thenReturn(userRepositoryResultList);

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
