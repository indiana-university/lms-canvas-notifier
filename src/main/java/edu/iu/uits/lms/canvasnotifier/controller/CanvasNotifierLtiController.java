package edu.iu.uits.lms.canvasnotifier.controller;

import edu.iu.uits.lms.canvasnotifier.model.User;
import edu.iu.uits.lms.canvasnotifier.repository.UserRepository;
import edu.iu.uits.lms.lti.controller.LtiController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tsugi.basiclti.BasicLTIConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping({"/lti"})
@Slf4j
public class CanvasNotifierLtiController extends LtiController {

    @Autowired
    private UserRepository userRepository;

    private boolean openLaunchUrlInNewWindow = false;

    @Override
    protected String getLaunchUrl(Map<String, String> launchParams) {
        return "/app/main";
    }

    @Override
    protected Map<String, String> getParametersForLaunch(Map<String, String> payload, Claims claims) {
        Map<String, String> paramMap = new HashMap<String, String>(1);

        paramMap.put(CUSTOM_CANVAS_COURSE_ID, payload.get(CUSTOM_CANVAS_COURSE_ID));
        paramMap.put(BasicLTIConstants.ROLES, payload.get(BasicLTIConstants.ROLES));
        paramMap.put(CUSTOM_CANVAS_USER_LOGIN_ID, payload.get(CUSTOM_CANVAS_USER_LOGIN_ID));

        openLaunchUrlInNewWindow = Boolean.valueOf(payload.get(CUSTOM_OPEN_IN_NEW_WINDOW));

        return paramMap;
    }

    @Override
    protected void preLaunchSetup(Map<String, String> launchParams, HttpServletRequest request, HttpServletResponse response) {
        String userId = launchParams.get(CUSTOM_CANVAS_USER_LOGIN_ID);

        String rolesString = "NotAuthorized";

        User user = userRepository.findByUsername(userId);

        if (user != null && user.isAuthorizedUser()) {
            rolesString = "Instructor";
        }

        String[] userRoles = rolesString.split(",");
        String authority = returnEquivalentAuthority(Arrays.asList(userRoles), getDefaultInstructorRoles());
        log.debug("LTI equivalent authority: " + authority);

        String systemId = launchParams.get(BasicLTIConstants.TOOL_CONSUMER_INSTANCE_GUID);
        String courseId = launchParams.get(CUSTOM_CANVAS_COURSE_ID);

        LtiAuthenticationToken token = new LtiAuthenticationToken(userId,
                courseId, systemId, AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, authority), getToolContext());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Override
    protected String getToolContext() {
        return "lms_lti_canvasnotifier";
    }

    @Override
    protected LAUNCH_MODE launchMode() {
        if (openLaunchUrlInNewWindow)
            return LAUNCH_MODE.WINDOW;

        return LAUNCH_MODE.FORWARD;
    }
}
