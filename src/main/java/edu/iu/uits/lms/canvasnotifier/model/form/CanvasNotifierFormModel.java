package edu.iu.uits.lms.canvasnotifier.model.form;

import edu.iu.uits.lms.canvasnotifier.model.User;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Data
public class CanvasNotifierFormModel {

    private List<User> userList;

    private String selectedSenderCanvasId;
    private String selectedSenderDisplayName;

    private String subject;

    private String body;
    private String previewBody;

    private MultipartFile cnAttachment;

    private String cnAttachmentText;

    private List<String> globalErrorsList;

    private Map<String, Boolean> fieldErrorsMap;

    boolean successfullySubmitted;

    public void clearAllFields() {
        this.userList = null;
        this.selectedSenderCanvasId = null;
        this.selectedSenderDisplayName = null;
        this.subject = null;
        this.body = null;
        this.previewBody = null;
        this.cnAttachment = null;
        this.cnAttachmentText = null;
        this.globalErrorsList = null;
        this.fieldErrorsMap = null;
    }

}
