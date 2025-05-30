package edu.iu.uits.lms.canvasnotifier.controller;

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
import com.opencsv.CSVReader;
import edu.iu.uits.lms.canvasnotifier.Constants;
import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessage;
import edu.iu.uits.lms.canvasnotifier.amqp.CanvasNotifierMessageSender;
import edu.iu.uits.lms.canvasnotifier.model.Job;
import edu.iu.uits.lms.canvasnotifier.model.JobStatus;
import edu.iu.uits.lms.canvasnotifier.model.form.CanvasNotifierFormModel;
import edu.iu.uits.lms.canvasnotifier.repository.JobRepository;
import edu.iu.uits.lms.canvasnotifier.util.CanvasNotifierUtils;
import edu.iu.uits.lms.iuonly.model.acl.AuthorizedUser;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/app")
@Slf4j
public class CanvasNotifierController extends OidcTokenAwareController {
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CanvasNotifierMessageSender canvasNotifierMessageSender;

    @Autowired
    private AuthorizedUserService authorizedUserService;

    @RequestMapping({"/launch", "/main"})
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String main(Model model, CanvasNotifierFormModel canvasNotifierFormModel) {

        List<AuthorizedUser> userList = new ArrayList<>(authorizedUserService.findActiveUsersByPermission(Constants.AUTH_SENDER_TOOL_PERMISSION));
        userList.sort(Comparator.comparing(AuthorizedUser::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        AuthorizedUser pickOptionDummyUser = new AuthorizedUser();
        pickOptionDummyUser.setId(-1L);
        pickOptionDummyUser.setDisplayName("Choose a username...");
        userList.addFirst(pickOptionDummyUser);

        canvasNotifierFormModel.setUserList(userList);

        model.addAttribute("canvasNotifierFormModel", canvasNotifierFormModel);

        return "main";
    }

    @RequestMapping(value = "/preview", method = RequestMethod.POST)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String preview(Model model, @ModelAttribute CanvasNotifierFormModel canvasNotifierFormModel) throws Exception {
        canvasNotifierFormModel.setGlobalErrorsList(new ArrayList<>());
        canvasNotifierFormModel.setFieldErrorsMap(new HashMap<>());
        canvasNotifierFormModel.setSuccessfullySubmitted(false);
        canvasNotifierFormModel.setCnAttachmentText(null);

        if (canvasNotifierFormModel.getSelectedSenderCanvasId() == null ||
                canvasNotifierFormModel.getSelectedSenderCanvasId().isEmpty() || canvasNotifierFormModel.getSelectedSenderCanvasId().equals("-1")) {
            canvasNotifierFormModel.getFieldErrorsMap().put("sender", true);
        } else {
            canvasNotifierFormModel.setSelectedSenderDisplayName(authorizedUserService
                    .findByActiveCanvasUserIdAndToolPermission(canvasNotifierFormModel.getSelectedSenderCanvasId(), Constants.AUTH_SENDER_TOOL_PERMISSION)
                    .getDisplayName());
        }

        if (canvasNotifierFormModel.getSubject() == null || canvasNotifierFormModel.getSubject().isEmpty()) {
            canvasNotifierFormModel.getFieldErrorsMap().put("subject", true);
        }

        if (canvasNotifierFormModel.getBody() == null || canvasNotifierFormModel.getBody().isEmpty()) {
            canvasNotifierFormModel.getFieldErrorsMap().put("body", true);
        }
        
        MultipartFile cnAttachment = canvasNotifierFormModel.getCnAttachment();

        if (cnAttachment.isEmpty()) {
            canvasNotifierFormModel.getFieldErrorsMap().put("attachment", true);
        } else {
            CSVReader csvReader = new CSVReader(new InputStreamReader(cnAttachment.getInputStream()));
            List<String[]> rawContents = csvReader.readAll();

            if (rawContents == null || rawContents.isEmpty()) {
                canvasNotifierFormModel.getGlobalErrorsList().add("No CSV content found");
            }

            canvasNotifierFormModel.setCnAttachmentText(new Gson().toJson(rawContents));


            // check for duplicate header column names
            Map<String, String> duplicateHeaderNameValue = new HashMap<>();
            String[] headerLine = rawContents.getFirst();

            for (String header : headerLine) {
                if (duplicateHeaderNameValue.containsKey(header.toLowerCase())) {
                    canvasNotifierFormModel.getGlobalErrorsList().add("Duplicate header column named " + header + " found");
                } else {
                    duplicateHeaderNameValue.put(header.toLowerCase(), header.toLowerCase());
                }
            }

            // make sure the csv has a username column
            Map<String, String> lineMappedContent = CanvasNotifierUtils.createCsvLineDataMap(rawContents, 1);

            if (lineMappedContent == null || ! lineMappedContent.containsKey(CanvasNotifierUtils.USERNAME_COLUMN_NAME)) {
                canvasNotifierFormModel.getGlobalErrorsList().add("No username column defined in csv file");
            } else {
                canvasNotifierFormModel.setPreviewBody(CanvasNotifierUtils.getVariableReplacedBody(lineMappedContent, canvasNotifierFormModel.getBody()));
            }
        }

        if (! canvasNotifierFormModel.getGlobalErrorsList().isEmpty() || ! canvasNotifierFormModel.getFieldErrorsMap().isEmpty()) {
            return main(model, canvasNotifierFormModel);
        } else {
            return "preview";
        }
    }

    @RequestMapping(value = "/backorsend", method = RequestMethod.POST)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String back(Model model, @ModelAttribute CanvasNotifierFormModel canvasNotifierFormModel, @RequestParam String action) {
        canvasNotifierFormModel.setSuccessfullySubmitted(false);

        if ("send".equals(action)) {
            return send(model, canvasNotifierFormModel);
        } else {
            return main(model, canvasNotifierFormModel);
        }
    }

    @RequestMapping(value = "/send", method = RequestMethod.POST)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String send(Model model, @ModelAttribute CanvasNotifierFormModel canvasNotifierFormModel) {
        OidcAuthenticationToken token = getTokenWithoutContext();
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String currentUserLoginId = oidcTokenUtils.getUserLoginId();

        Job newJob = new Job();
        newJob.setInitited_by_username(currentUserLoginId);
        newJob.setSender_canvasid(canvasNotifierFormModel.getSelectedSenderCanvasId());
        newJob.setSubject(canvasNotifierFormModel.getSubject());
        newJob.setBody(canvasNotifierFormModel.getBody());
        newJob.setJson_csv(canvasNotifierFormModel.getCnAttachmentText());
        newJob.setStatus(JobStatus.PENDING);

        jobRepository.save(newJob);

        CanvasNotifierMessage canvasNotifierMessage = new CanvasNotifierMessage();
        canvasNotifierMessage.setId(newJob.getId());

        canvasNotifierMessageSender.send(canvasNotifierMessage);

        canvasNotifierFormModel.clearAllFields();
        canvasNotifierFormModel.setSuccessfullySubmitted(true);

        return main(model, canvasNotifierFormModel);
    }
}
