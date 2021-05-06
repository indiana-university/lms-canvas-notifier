package edu.iu.uits.lms.canvasnotifier.rest;

import edu.iu.uits.lms.canvasnotifier.model.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * RestUser extends User, but changes the boolean fields to Boolean so that the controller can check for
 * null on the update
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RestUser extends User implements Serializable {

    private Boolean authorizedSender;
    private Boolean authorizedUser;
}
